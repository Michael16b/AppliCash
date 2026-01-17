package fr.univ.nantes.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

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
    val description: String,
    val amount: Double,
    val paidBy: String
)

data class ExpenseState(
    val groupName: String = "",
    val participants: List<String> = emptyList(),
    val expenses: List<Expense> = emptyList()
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
class ExpenseViewModel : ViewModel() {

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
            val expense = Expense(description, amount, paidBy)
            _state.update { it.copy(
                expenses = it.expenses + expense
            ) }
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
    private fun calculateBalances(): List<Balance> {
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
    private fun calculateReimbursements(): List<Reimbursement> {
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
     * This clears the group name, all participants, and all expenses.
     */
    fun reset() {
        _state.update { ExpenseState() }
    }
}
