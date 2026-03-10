package fr.univ.nantes.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ.nantes.data.currency.ICurrencyRepository
import fr.univ.nantes.data.expense.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import fr.univ.nantes.domain.profil.ProfileUseCase
import org.json.JSONObject
import fr.univ.nantes.domain.profil.normalizeCurrencyCode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Represents an expense in the group.
 *
 * Note: This uses Double for currency amounts. While BigDecimal would provide better
 * precision for financial calculations, Double is acceptable for this use case as:
 * 1. The BALANCE_THRESHOLD constant helps mitigate display issues from floating-point errors
 * 2. The typical expense amounts won't accumulate significant precision errors
 * 3. The simplicity of Double makes the code more readable and performant
 *
 * For production financial applications, consider using BigDecimal or storing amounts
 * as Long representing the smallest currency unit (e.g., cents).
 */
data class Expense(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val paidBy: String,
    val splitType: Int = 0, // 0=Equally, 1=By share, 2=By amount
    val splitDetails: Map<String, Double> = emptyMap() // participant -> amount/share
)

data class GroupData(
    val id: Long = 0,
    val groupName: String = "",
    val participants: List<String> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

data class ExpenseState(
    val groupName: String = "",
    val participants: List<String> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val groups: List<GroupData> = emptyList(),
    val currentGroupId: Long? = null,
    val currentUserName: String? = null,
    val isLoggedIn: Boolean = false,
    val userCurrencyCode: String = "EUR",
    /** Age in minutes of the cached exchange rates, null if no cache or same currency. */
    val cacheAgeMinutes: Long? = null,
    /** List of currency codes available from the local DB cache. */
    val availableCurrencies: List<String> = emptyList(),
    /** Converted amount from selected expense currency to user base currency (for live preview). */
    val convertedAmountInBase: Double? = null
)

data class Balance(
    val participant: String,
    val amount: Double
)

data class Reimbursement(
    val from: String,
    val to: String,
    val amount: Double
)

sealed class ExpenseEvent {
    data class ShowSnackbar(val message: String) : ExpenseEvent()
}

/**
 * ViewModel for managing group expenses and calculating balances.
 *
 * This ViewModel maintains the state of a group expense tracker, including
 * the group name, list of participants, and individual expenses. It provides
 * functionality to calculate balances and determine optimal reimbursements.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val profileUseCase: ProfileUseCase,
    private val currencyRepository: ICurrencyRepository
) : ViewModel() {

    companion object {
        /**
         * Threshold for floating-point comparisons to handle precision issues in currency calculations.
         * This value (0.01) represents one cent and is used to avoid issues with floating-point arithmetic
         * when comparing monetary amounts.
         */
        private const val BALANCE_THRESHOLD = 0.01

        /**
         * Helper function to parse JSON splitDetails string to Map<String, Double>
         */
        fun parseSplitDetails(jsonString: String): Map<String, Double> {
            return try {
                if (jsonString.isEmpty() || jsonString == "{}") return emptyMap()
                val jsonObject = JSONObject(jsonString)
                jsonObject.keys().asSequence().associate { key ->
                    key to jsonObject.getDouble(key)
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        /**
         * Helper function to convert Map<String, Double> to JSON string
         */
        fun serializeSplitDetails(details: Map<String, Double>): String {
            if (details.isEmpty()) return "{}"
            val jsonObject = JSONObject()
            details.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            return jsonObject.toString()
        }

        /**
         * The currency in which expenses are stored in the database.
         * All stored amounts are in this currency and will be converted to the user's preferred currency for display.
         */
        const val STORAGE_CURRENCY = "EUR"
    }

    private val _state = MutableStateFlow(ExpenseState())
    val state: StateFlow<ExpenseState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ExpenseEvent>()
    val events: SharedFlow<ExpenseEvent> = _events.asSharedFlow()

    /**
     * Groups with amounts converted to the user's preferred currency.
     * Uses flatMapLatest so that each time groups or currency changes, a new suspend
     * conversion is triggered and the old one is cancelled.
     */
    val convertedGroups: StateFlow<List<GroupData>> = _state
        .map { it.groups to it.userCurrencyCode }
        .flatMapLatest { (groups, targetCurrency) ->
            flow {
                if (targetCurrency == STORAGE_CURRENCY) {
                    _state.update { it.copy(cacheAgeMinutes = null) }
                    emit(groups)
                } else {
                    var hadConversionError = false
                    val converted = groups.map { group ->
                        group.copy(
                            expenses = group.expenses.map { expense ->
                                val c = currencyRepository.convert(expense.amount, STORAGE_CURRENCY, targetCurrency)
                                if (c != null) {
                                    expense.copy(amount = c)
                                } else {
                                    hadConversionError = true
                                    expense
                                }
                            }
                        )
                    }
                    val ageMinutes = currencyRepository.getCacheAgeMinutes(STORAGE_CURRENCY)
                    _state.update { it.copy(cacheAgeMinutes = ageMinutes) }
                    if (hadConversionError) {
                        viewModelScope.launch {
                            val msg = if (ageMinutes != null) {
                                "Données de conversion de il y a $ageMinutes min (hors connexion)"
                            } else {
                                "Impossible de convertir les devises. Vérifiez votre connexion Internet."
                            }
                            _events.emit(ExpenseEvent.ShowSnackbar(msg))
                        }
                    }
                    emit(converted)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load groups from the database
        viewModelScope.launch {
            repository.getAllGroupsWithDetails().collect { groupsWithDetails ->
                val groups = groupsWithDetails.map { groupWithDetails ->
                    GroupData(
                        id = groupWithDetails.group.id,
                        groupName = groupWithDetails.group.groupName,
                        participants = groupWithDetails.participants.map { it.name },
                        expenses = groupWithDetails.expenses.map {
                            Expense(
                                id = it.id,
                                description = it.description,
                                amount = it.amount,
                                paidBy = it.paidBy,
                                splitType = it.splitType,
                                splitDetails = parseSplitDetails(it.splitDetails)
                            )
                        }
                    )
                }
                _state.update { it.copy(groups = groups) }
            }
        }
        viewModelScope.launch {
            profileUseCase.observeProfile().collect { profile ->
                _state.update {
                    it.copy(
                        currentUserName = profile?.firstName,
                        isLoggedIn = profile?.isLoggedIn == true,
                        userCurrencyCode = profile?.currency?.normalizeCurrencyCode() ?: "EUR"
                    )
                }
            }
        }
        // Load available currencies from the local DB (triggers network fetch if needed)
        viewModelScope.launch {
            val currencies = currencyRepository.getAvailableCurrencies(STORAGE_CURRENCY)
            if (currencies.isNotEmpty()) {
                _state.update { it.copy(availableCurrencies = currencies) }
            }
        }
    }

    /**
     * Converts [amount] from [fromCurrency] to the user's base currency and stores the
     * result in [ExpenseState.convertedAmountInBase] for live preview in the form.
     * The target currency is always [STORAGE_CURRENCY] (the group's base currency).
     * Clears the preview if [fromCurrency] already equals [STORAGE_CURRENCY].
     * Emits a [ExpenseEvent.ShowSnackbar] if the conversion rate is unavailable.
     */
    fun updateLiveConversion(amount: Double, fromCurrency: String) {
        // Target is always the group storage currency (EUR)
        val targetCurrency = STORAGE_CURRENCY
        if (fromCurrency == targetCurrency || amount <= 0.0) {
            _state.update { it.copy(convertedAmountInBase = null) }
            return
        }
        viewModelScope.launch {
            // The EUR-based rate cache contains EUR→X entries.
            // To convert X→EUR, use the inverse: 1/rate(EUR→X).
            val eurToFrom = currencyRepository.getRate(targetCurrency, fromCurrency)
            val converted: Double? = if (eurToFrom != null && eurToFrom != 0.0) {
                amount / eurToFrom
            } else {
                // Fallback: try direct network-based conversion
                currencyRepository.convert(amount, fromCurrency, targetCurrency)
            }

            if (converted == null) {
                _events.emit(
                    ExpenseEvent.ShowSnackbar(
                        "Impossible de convertir $fromCurrency → $targetCurrency. Vérifiez votre connexion Internet."
                    )
                )
            }
            _state.update { it.copy(convertedAmountInBase = converted) }
        }
    }

    /**
     * Current group's expenses converted to the user's preferred currency.
     * Uses flatMapLatest to properly execute suspend conversion calls.
     */
    val convertedCurrentExpenses: StateFlow<List<Expense>> = _state
        .map { it.expenses to it.userCurrencyCode }
        .flatMapLatest { (expenses, targetCurrency) ->
            flow {
                if (targetCurrency == STORAGE_CURRENCY) {
                    emit(expenses)
                } else {
                    var hadConversionError = false
                    val converted = expenses.map { expense ->
                        val c = currencyRepository.convert(expense.amount, STORAGE_CURRENCY, targetCurrency)
                        if (c != null) {
                            expense.copy(amount = c)
                        } else {
                            hadConversionError = true
                            expense
                        }
                    }
                    if (hadConversionError) {
                        val ageMinutes = currencyRepository.getCacheAgeMinutes(STORAGE_CURRENCY)
                        viewModelScope.launch {
                            val msg = if (ageMinutes != null) {
                                "Données de conversion de il y a $ageMinutes min (hors connexion)"
                            } else {
                                "Impossible de convertir les devises. Vérifiez votre connexion Internet."
                            }
                            _events.emit(ExpenseEvent.ShowSnackbar(msg))
                        }
                    }
                    emit(converted)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Derived state flow for balances, automatically recalculated when expenses or participants change.
     * Uses converted amounts for display consistency.
     */
    val balances: StateFlow<List<Balance>> = combine(
        convertedCurrentExpenses,
        _state.map { it.participants }
    ) { convertedExpenses, participants ->
        calculateBalancesFrom(convertedExpenses, participants)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Derived state flow for reimbursements, automatically recalculated when balances change.
     */
    val reimbursements: StateFlow<List<Reimbursement>> = balances.map { calculateReimbursementsFrom(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Loads an existing group from Room and updates the current state
     * to allow adding expenses to that group.
     *
     * @param groupId The ID of the group to load
     */
    fun loadGroup(groupId: Long) {
        viewModelScope.launch {
            val groupWithDetails = repository.getGroupWithDetails(groupId)
            if (groupWithDetails != null) {
                _state.update {
                    it.copy(
                        groupName = groupWithDetails.group.groupName,
                        participants = groupWithDetails.participants.map { participant -> participant.name },
                        expenses = groupWithDetails.expenses.map {
                            Expense(
                                id = it.id,
                                description = it.description,
                                amount = it.amount,
                                paidBy = it.paidBy,
                                splitType = it.splitType,
                                splitDetails = parseSplitDetails(it.splitDetails)
                            )
                        },
                        currentGroupId = groupId
                    )
                }
            }
        }
    }

    /**
     * Returns the ID of the currently loaded group, or null if no group is loaded.
     */
    fun getCurrentGroupId(): Long? {
        return _state.value.currentGroupId
    }

    /**
     * Sets the name of the expense group.
     *
     * @param name The name to assign to the group
     */
    fun setGroupName(name: String) {
        _state.update { it.copy(groupName = name) }
    }

    /**
     * Adds a new participant to the group.
     *
     * The participant will only be added if the name is not blank and not already in the group.
     * The name is trimmed before validation.
     *
     * @param name The name of the participant to add
     */
    fun addParticipant(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotBlank() && !_state.value.participants.contains(trimmedName)) {
            _state.update { it.copy(
                participants = it.participants + trimmedName
            ) }
        }
    }

    /**
     * Removes a participant from the group.
     *
     * When a participant is removed, all expenses paid by that participant are also removed
     * to maintain data consistency and ensure accurate balance calculations.
     *
     * @param name The name of the participant to remove
     */
    fun removeParticipant(name: String) {
        _state.update { it.copy(
            participants = it.participants - name,
            expenses = it.expenses.filter { expense -> expense.paidBy != name }
        ) }
    }

    /**
     * Adds a new expense to the group.
     *
     * The expense will only be added if:
     * - The description is not blank
     * - The amount is greater than 0
     * - The payer name is not blank
     * - The payer is a current participant in the group
     *
     * @param description A description of the expense
     * @param amount The amount of the expense
     * @param paidBy The name of the participant who paid for the expense
     */
    fun addExpense(
        description: String,
        amount: Double,
        paidBy: String,
        splitType: Int = 0,
        splitDetails: Map<String, Double> = emptyMap()
    ) {
        if (description.isNotBlank() && amount > 0 && paidBy.isNotBlank() && _state.value.participants.contains(paidBy)) {
            val expense = Expense(
                description = description,
                amount = amount,
                paidBy = paidBy,
                splitType = splitType,
                splitDetails = splitDetails
            )
            _state.update { it.copy(expenses = it.expenses + expense) }

            val groupId = getCurrentGroupId()
            if (groupId != null) {
                viewModelScope.launch {
                    repository.addExpenseToGroup(
                        groupId = groupId,
                        description = description,
                        amount = amount,
                        paidBy = paidBy,
                        splitType = splitType,
                        splitDetails = serializeSplitDetails(splitDetails)
                    )
                }
            }
        }
    }

    /**
     * Calculates the balance for each participant from the given [expenses] and [participants].
     */
    private fun calculateBalancesFrom(expenses: List<Expense>, participants: List<String>): List<Balance> {
        if (participants.isEmpty()) return emptyList()

        val total = expenses.sumOf { it.amount }
        val share = total / participants.size

        val paidByParticipant = participants.associateWith { participant ->
            expenses.filter { it.paidBy == participant }.sumOf { it.amount }
        }

        return participants.map { participant ->
            Balance(participant, (paidByParticipant[participant] ?: 0.0) - share)
        }
    }

    /**
     * Calculates balances using the raw (non-converted) state values.
     * Used by unit tests.
     */
    fun calculateBalances(): List<Balance> =
        calculateBalancesFrom(_state.value.expenses, _state.value.participants)

    /**
     * Calculates optimal reimbursements from the given [balances].
     */
    private fun calculateReimbursementsFrom(balances: List<Balance>): List<Reimbursement> {
        val reimbursements = mutableListOf<Reimbursement>()

        val debtors = balances.filter { it.amount < 0 }.sortedBy { it.amount }.toMutableList()
        val creditors = balances.filter { it.amount > 0 }.sortedByDescending { it.amount }.toMutableList()

        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]

            val amount = minOf(-debtor.amount, creditor.amount)

            if (amount > BALANCE_THRESHOLD) {
                reimbursements.add(Reimbursement(debtor.participant, creditor.participant, amount))
            }

            debtors[i] = debtor.copy(amount = debtor.amount + amount)
            creditors[j] = creditor.copy(amount = creditor.amount - amount)

            if (debtors[i].amount >= -BALANCE_THRESHOLD) i++
            if (creditors[j].amount <= BALANCE_THRESHOLD) j++
        }

        return reimbursements
    }

    /**
     * Calculates reimbursements using the raw (non-converted) state values.
     * Used by unit tests.
     */
    fun calculateReimbursements(): List<Reimbursement> =
        calculateReimbursementsFrom(calculateBalances())


    /**
     * Resets the ViewModel to its initial state.
     *
     * This clears the group name, all participants, and all expenses,
     * but preserves the list of saved groups.
     */
    fun reset() {
        _state.update { currentState ->
            ExpenseState().copy(groups = currentState.groups)
        }
    }

    /**
     * Saves the current group as a complete group and adds it to the groups list.
     * This clears the current working state for creating a new group.
     *
     * Normalizes participant names (trim) and removes duplicates before persisting.
     */
    fun saveGroup() {
        val currentGroup = _state.value
        // Normalize participant names: trim and remove duplicates
        val normalizedParticipants = currentGroup.participants
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (currentGroup.groupName.isNotBlank() && normalizedParticipants.isNotEmpty()) {
            viewModelScope.launch {
                val groupId = repository.createGroup(
                    groupName = currentGroup.groupName,
                    participants = normalizedParticipants
                )

                currentGroup.expenses.forEach { expense ->
                    repository.addExpenseToGroup(
                        groupId = groupId,
                        description = expense.description,
                        amount = expense.amount,
                        paidBy = expense.paidBy,
                        splitType = expense.splitType,
                        splitDetails = serializeSplitDetails(expense.splitDetails)
                    )
                }

                _state.update { it.copy(
                    groupName = "",
                    participants = emptyList(),
                    expenses = emptyList(),
                    currentGroupId = null
                ) }
            }
        }
    }

    /**
     * Converts [amount] from currency [from] to the user's preferred currency.
     * Returns null if the exchange rate is unavailable.
     */
    suspend fun convertAmount(amount: Double, from: String): Double? {
        val targetCurrency = _state.value.userCurrencyCode
        return currencyRepository.convert(amount, from, targetCurrency)
    }

    /**
     * Deletes an expense by id from the repository and reloads the given group.
     */
    fun deleteExpense(expenseId: Long, groupId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            loadGroup(groupId)
        }
    }

    /**
     * Updates the name of an existing group in the database.
     */
    fun updateGroupName(groupId: Long, newName: String) {
        viewModelScope.launch {
            repository.updateGroupName(groupId, newName)
            if (_state.value.currentGroupId == groupId) {
                _state.update { it.copy(groupName = newName) }
            }
        }
    }

    /**
     * Adds a participant to an existing group in the database.
     * The name is trimmed and checked for duplicates before being added.
     */
    fun addParticipantToGroup(groupId: Long, participantName: String) {
        val trimmedName = participantName.trim()
        if (trimmedName.isNotBlank() && !_state.value.participants.contains(trimmedName)) {
            viewModelScope.launch {
                repository.addParticipantToGroup(groupId, trimmedName)
                loadGroup(groupId)
            }
        }
    }

    /**
     * Removes a participant from an existing group in the database.
     */
    fun removeParticipantFromGroup(groupId: Long, participantName: String) {
        viewModelScope.launch {
            repository.removeParticipantFromGroup(groupId, participantName)
            loadGroup(groupId)
        }
    }

    /**
     * Applies all group edits (name change, added/removed participants) in a single batch,
     * then refreshes the current group state once to avoid redundant DB reads.
     */
    fun updateGroup(
        groupId: Long,
        newName: String?,
        addParticipants: List<String>,
        removeParticipants: List<String>
    ) {
        viewModelScope.launch {
            repository.updateGroup(groupId, newName, addParticipants, removeParticipants)
            if (newName != null && _state.value.currentGroupId == groupId) {
                _state.update { it.copy(groupName = newName) }
            }
        }
    }

}
