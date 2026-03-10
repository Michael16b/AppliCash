package fr.univ.nantes.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ.nantes.data.expense.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import fr.univ.nantes.domain.profil.ProfileUseCase

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
    val paidBy: String
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
    val isLoggedIn: Boolean = false
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

/**
 * ViewModel for managing group expenses and calculating balances.
 *
 * This ViewModel maintains the state of a group expense tracker, including
 * the group name, list of participants, and individual expenses. It provides
 * functionality to calculate balances and determine optimal reimbursements.
 */
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val profileUseCase: ProfileUseCase
) : ViewModel() {

    companion object {
        /**
         * Threshold for floating-point comparisons to handle precision issues in currency calculations.
         * This value (0.01) represents one cent and is used to avoid issues with floating-point arithmetic
         * when comparing monetary amounts.
         */
        private const val BALANCE_THRESHOLD = 0.01
    }

    private val _state = MutableStateFlow(ExpenseState())
    val state: StateFlow<ExpenseState> = _state.asStateFlow()

    init {
        // Charger les groupes depuis la base de données
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
                                paidBy = it.paidBy
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
                        isLoggedIn = profile?.isLoggedIn == true
                    )
                }
            }
        }
    }

    /**
     * Derived state flow for balances, automatically recalculated when expenses or participants change.
     */
    val balances: StateFlow<List<Balance>> = _state.map { calculateBalances() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Derived state flow for reimbursements, automatically recalculated when balances change.
     */
    val reimbursements: StateFlow<List<Reimbursement>> = _state.map { calculateReimbursements() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Charge un groupe existant depuis Room et met à jour l'état actuel
     * pour permettre l'ajout de dépenses à ce groupe.
     */
    fun loadGroup(groupId: Long) {
        viewModelScope.launch {
            val groupWithDetails = repository.getGroupWithDetails(groupId)
            if (groupWithDetails != null) {
                _state.update {
                    it.copy(
                        groupName = groupWithDetails.group.groupName,
                        participants = groupWithDetails.participants.map { participant -> participant.name },
                        expenses = groupWithDetails.expenses.map { expense ->
                            Expense(
                                id = expense.id,
                                description = expense.description,
                                amount = expense.amount,
                                paidBy = expense.paidBy
                            )
                        },
                        currentGroupId = groupId
                    )
                }
            }
        }
    }

    /**
     * Retourne l'ID du groupe actuellement chargé
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
     *
     * @param name The name of the participant to add
     */
    fun addParticipant(name: String) {
        if (name.isNotBlank() && !_state.value.participants.contains(name)) {
            _state.update { it.copy(
                participants = it.participants + name
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
    fun addExpense(description: String, amount: Double, paidBy: String) {
        if (description.isNotBlank() && amount > 0 && paidBy.isNotBlank() && _state.value.participants.contains(paidBy)) {
            val expense = Expense(description = description, amount = amount, paidBy = paidBy)
            _state.update { it.copy(
                expenses = it.expenses + expense
            ) }


            val groupId = getCurrentGroupId()
            if (groupId != null) {
                viewModelScope.launch {
                    repository.addExpenseToGroup(
                        groupId = groupId,
                        description = description,
                        amount = amount,
                        paidBy = paidBy
                    )
                }
            }
        }
    }

    /**
     * Calculates the balance for each participant.
     *
     * The balance represents how much each participant has overpaid (positive balance)
     * or underpaid (negative balance) relative to their fair share of the total expenses.
     * The fair share is calculated by dividing the total expenses equally among all participants.
     *
     * @return A list of Balance objects, one for each participant
     */
    fun calculateBalances(): List<Balance> {
        val participants = _state.value.participants
        val expenses = _state.value.expenses

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
     * Calculates optimal reimbursements to settle all balances.
     *
     * This method uses a greedy algorithm to minimize the number of transactions needed
     * to settle all debts within the group. It matches debtors (those who owe money) with
     * creditors (those who are owed money) to determine the most efficient payment plan.
     *
     * Only reimbursements above the BALANCE_THRESHOLD are included to avoid trivial transactions
     * caused by floating-point precision issues.
     *
     * @return A list of Reimbursement objects representing the payments needed to settle all balances
     */
    fun calculateReimbursements(): List<Reimbursement> {
        val balances = calculateBalances().toMutableList()
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
     */
    fun saveGroup() {
        val currentGroup = _state.value
        if (currentGroup.groupName.isNotBlank() && currentGroup.participants.isNotEmpty()) {
            viewModelScope.launch {
                val groupId = repository.createGroup(
                    groupName = currentGroup.groupName,
                    participants = currentGroup.participants
                )

                currentGroup.expenses.forEach { expense ->
                    repository.addExpenseToGroup(
                        groupId = groupId,
                        description = expense.description,
                        amount = expense.amount,
                        paidBy = expense.paidBy
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
     */
    fun addParticipantToGroup(groupId: Long, participantName: String) {
        viewModelScope.launch {
            repository.addParticipantToGroup(groupId, participantName)
            loadGroup(groupId)
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

}
