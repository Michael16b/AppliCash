package fr.univ.nantes.domain.expense

/**
 * Pure domain service that encapsulates all balance and reimbursement calculations.
 *
 * All methods are stateless and work on plain domain models — no Android or Room dependency.
 *
 * Business rules enforced:
 *   BR7  – positive balance = credit, negative balance = debt
 *   BR8  – split shares must total 100 % of the expense amount
 *   BR10 – balances are computed from ALL expenses in the group
 *   BR6  – the reimbursement algorithm minimises the number of transactions
 */
@Suppress("unused")
object ExpenseCalculator {

    /**
     * Floating-point tolerance used to avoid treating tiny rounding errors as real debts.
     * One cent (0.01) is a safe threshold for display and reimbursement purposes.
     */
    const val BALANCE_THRESHOLD = 0.01

    // ── Balance calculation ───────────────────────────────────────────────────

    /**
     * Calculates the net balance for every participant.
     *
     * BR7  – positive = credit, negative = debt.
     * BR10 – considers all [expenses] in the group.
     *
     * Equal split strategy: each participant owes (totalAmount / participantCount).
     * Each participant's balance = amountPaid – fairShare.
     *
     * @param expenses   All expenses of the group (BR10).
     * @param participants All members of the group.
     * @return List of [Balance] objects, one per participant.
     */
    fun calculateBalances(expenses: List<Expense>, participants: List<String>): List<Balance> {
        if (participants.isEmpty()) return emptyList()

        val total = expenses.sumOf { it.amount }
        val equalShare = total / participants.size

        val paidByParticipant = participants.associateWith { name ->
            expenses.filter { it.paidBy == name }.sumOf { it.amount }
        }

        return participants.map { name ->
            Balance(name, (paidByParticipant[name] ?: 0.0) - equalShare)
        }
    }

    /**
     * Calculates balances using a custom per-expense split strategy.
     *
     * Each [Expense] carries a [Expense.splitDetails] map defining each participant's
     * share of that expense. BR8 is enforced: the sum of all shares must equal the
     * total expense amount (within [BALANCE_THRESHOLD]).
     *
     * @throws ExpenseDomainException.InvalidSplitException if any expense's shares don't sum correctly (BR8).
     */
    fun calculateBalancesWithSplit(expenses: List<Expense>, participants: List<String>): List<Balance> {
        if (participants.isEmpty()) return emptyList()

        // BR8: validate every expense with explicit split details
        expenses.forEach { expense ->
            if (expense.splitDetails.isNotEmpty()) {
                val splitSum = expense.splitDetails.values.sum()
                if (Math.abs(splitSum - expense.amount) > BALANCE_THRESHOLD) {
                    throw ExpenseDomainException.InvalidSplitException(
                        "Expense '${expense.description}': split sum $splitSum ≠ amount ${expense.amount}"
                    )
                }
            }
        }

        val net = participants.associateWith { 0.0 }.toMutableMap()

        expenses.forEach { expense ->
            // Credit the payer
            net[expense.paidBy] = (net[expense.paidBy] ?: 0.0) + expense.amount

            if (expense.splitDetails.isNotEmpty()) {
                // Debit each participant by their explicit share
                expense.splitDetails.forEach { (participant, share) ->
                    net[participant] = (net[participant] ?: 0.0) - share
                }
            } else {
                // Equal split fallback
                val share = expense.amount / participants.size
                participants.forEach { p ->
                    net[p] = (net[p] ?: 0.0) - share
                }
            }
        }

        return participants.map { Balance(it, net[it] ?: 0.0) }
    }

    // ── Reimbursement algorithm ───────────────────────────────────────────────

    /**
     * Computes the minimum number of reimbursement transactions needed to settle all debts.
     *
     * BR6 – greedy two-pointer algorithm on sorted debtors / creditors:
     *   1. Sort debtors ascending (most negative first).
     *   2. Sort creditors descending (most positive first).
     *   3. At each step, settle as much as possible between the current debtor and creditor.
     *   4. Advance the pointer of whoever is fully settled.
     *
     * Amounts below [BALANCE_THRESHOLD] are ignored to avoid micro-transactions caused by
     * floating-point rounding.
     *
     * @param balances Pre-computed [Balance] list (e.g. from [calculateBalances]).
     * @return Minimal list of [Reimbursement] transactions.
     */
    fun calculateReimbursements(balances: List<Balance>): List<Reimbursement> {
        val reimbursements = mutableListOf<Reimbursement>()

        // BR6: sort to allow greedy minimisation
        val debtors = balances
            .filter { it.amount < -BALANCE_THRESHOLD }
            .sortedBy { it.amount }
            .map { it.copy() }
            .toMutableList()
        val creditors = balances
            .filter { it.amount > BALANCE_THRESHOLD }
            .sortedByDescending { it.amount }
            .map { it.copy() }
            .toMutableList()

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

    // ── Split validation ──────────────────────────────────────────────────────

    /**
     * Validates that the shares in [splitDetails] sum to [totalAmount] within tolerance.
     * BR8: splits must total 100 % of the expense.
     *
     * @throws ExpenseDomainException.InvalidSplitException if the constraint is violated.
     */
    fun validateSplit(totalAmount: Double, splitDetails: Map<String, Double>) {
        if (splitDetails.isEmpty()) return
        val sum = splitDetails.values.sum()
        if (Math.abs(sum - totalAmount) > BALANCE_THRESHOLD) {
            throw ExpenseDomainException.InvalidSplitException(
                "Split sum $sum does not equal total amount $totalAmount"
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts [amount] using the provided exchange [rate].
     * BR9: applies the provided exchange rate directly (amount × rate).
     *
     * @param amount The amount to convert.
     * @param rate   The exchange rate to apply. Must be non-null and non-zero.
     * @return Converted amount, or null if [rate] is null or zero.
     */
    fun convertAmount(amount: Double, rate: Double?): Double? {
        if (rate == null || rate == 0.0) return null
        return amount * rate
    }
}
