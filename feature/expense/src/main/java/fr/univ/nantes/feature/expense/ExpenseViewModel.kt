package fr.univ.nantes.feature.expense

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class ExpenseViewModel : ViewModel() {

    companion object {
        // Threshold for floating-point comparisons to handle precision issues in currency calculations
        private const val BALANCE_THRESHOLD = 0.01
    }

    private val _state = MutableStateFlow(ExpenseState())
    val state: StateFlow<ExpenseState> = _state.asStateFlow()

    fun setGroupName(name: String) {
        _state.value = _state.value.copy(groupName = name)
    }

    fun addParticipant(name: String) {
        if (name.isNotBlank() && !_state.value.participants.contains(name)) {
            _state.value = _state.value.copy(
                participants = _state.value.participants + name
            )
        }
    }

    fun removeParticipant(name: String) {
        _state.value = _state.value.copy(
            participants = _state.value.participants - name
        )
    }

    fun addExpense(description: String, amount: Double, paidBy: String) {
        if (description.isNotBlank() && amount > 0 && paidBy.isNotBlank() && _state.value.participants.contains(paidBy)) {
            val expense = Expense(description, amount, paidBy)
            _state.value = _state.value.copy(
                expenses = _state.value.expenses + expense
            )
        }
    }

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

    fun reset() {
        _state.value = ExpenseState()
    }
}
