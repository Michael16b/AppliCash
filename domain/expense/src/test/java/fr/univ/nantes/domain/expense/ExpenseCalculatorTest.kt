package fr.univ.nantes.domain.expense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit tests for [ExpenseCalculator].
 *
 * Business rules covered:
 *   BR6  – reimbursement algorithm minimises the number of transactions
 *   BR7  – positive balance = credit, negative balance = debt
 *   BR8  – split shares must total 100 % of the expense amount
 *   BR9  – currency conversion uses the provided exchange rate
 *   BR10 – balances are computed from ALL expenses in the group
 *
 * Edge cases covered (CA5):
 *   – Empty participants / expenses
 *   – Single participant
 *   – Zero-amount expenses
 *   – Rounding artefacts (3 participants, amount not divisible evenly)
 *   – Already-balanced group (all balances zero)
 *   – Negative balance edge (debt larger than credit)
 */
class ExpenseCalculatorTest {

    private val delta = 0.001

    // ── calculateBalances — equal split ───────────────────────────────────────

    @Test
    fun `calculateBalances returns empty list when no participants`() {
        val result = ExpenseCalculator.calculateBalances(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateBalances with no expenses gives zero balance for everyone`() {
        val result = ExpenseCalculator.calculateBalances(
            emptyList(),
            listOf("Alice", "Bob", "Charlie")
        )
        assertEquals(3, result.size)
        result.forEach { assertEquals(0.0, it.amount, delta) }
    }

    @Test
    fun `BR10 - calculateBalances aggregates all group expenses`() {
        val expenses = listOf(
            Expense(description = "Dinner", amount = 60.0, paidBy = "Alice"),
            Expense(description = "Taxi", amount = 30.0, paidBy = "Bob")
        )
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob", "Charlie"))

        // Total = 90, share = 30 each
        // Alice paid 60 → balance = +30
        // Bob paid 30 → balance = 0
        // Charlie paid 0 → balance = -30
        val alice = result.first { it.participant == "Alice" }
        val bob = result.first { it.participant == "Bob" }
        val charlie = result.first { it.participant == "Charlie" }

        assertEquals(30.0, alice.amount, delta)
        assertEquals(0.0, bob.amount, delta)
        assertEquals(-30.0, charlie.amount, delta)
    }

    @Test
    fun `BR7 - positive balance means credit, negative means debt`() {
        val expenses = listOf(Expense(description = "Hotel", amount = 100.0, paidBy = "Alice"))
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob"))

        val alice = result.first { it.participant == "Alice" }
        val bob = result.first { it.participant == "Bob" }

        // Alice paid 100, share = 50 → Alice +50 (credit), Bob -50 (debt)
        assertTrue("Alice should have a positive balance", alice.amount > 0)
        assertTrue("Bob should have a negative balance", bob.amount < 0)
        assertEquals(50.0, alice.amount, delta)
        assertEquals(-50.0, bob.amount, delta)
    }

    @Test
    fun `calculateBalances handles a single participant with expenses`() {
        val expenses = listOf(Expense(description = "Groceries", amount = 40.0, paidBy = "Alice"))
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice"))

        // Total = 40, share = 40, Alice paid 40 → balance 0
        assertEquals(1, result.size)
        assertEquals(0.0, result[0].amount, delta)
    }

    @Test
    fun `calculateBalances handles rounding with 3 participants and indivisible amount`() {
        // 10 / 3 = 3.333... — sum of balances must still be 0
        val expenses = listOf(Expense(description = "Coffee", amount = 10.0, paidBy = "Alice"))
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob", "Charlie"))

        val sum = result.sumOf { it.amount }
        assertEquals(0.0, sum, delta)

        val alice = result.first { it.participant == "Alice" }
        assertTrue("Alice should have positive balance", alice.amount > 0)
    }

    @Test
    fun `calculateBalances returns all-zero balances when every participant has paid equally`() {
        val expenses = listOf(
            Expense(description = "Part 1", amount = 50.0, paidBy = "Alice"),
            Expense(description = "Part 2", amount = 50.0, paidBy = "Bob")
        )
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob"))
        result.forEach { assertEquals(0.0, it.amount, delta) }
    }

    @Test
    fun `calculateBalances sum of all balances is always zero`() {
        val expenses = listOf(
            Expense(description = "A", amount = 120.0, paidBy = "Alice"),
            Expense(description = "B", amount = 45.0, paidBy = "Bob"),
            Expense(description = "C", amount = 75.0, paidBy = "Alice")
        )
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob", "Charlie"))
        val sum = result.sumOf { it.amount }
        assertEquals(0.0, sum, delta)
    }

    // ── calculateBalancesWithSplit — custom split ──────────────────────────────

    @Test
    fun `BR8 - calculateBalancesWithSplit throws when split sum does not match amount`() {
        val expenses = listOf(
            Expense(
                description = "Dinner",
                amount = 90.0,
                paidBy = "Alice",
                splitDetails = mapOf("Alice" to 30.0, "Bob" to 30.0) // sums to 60, not 90
            )
        )
        try {
            ExpenseCalculator.calculateBalancesWithSplit(expenses, listOf("Alice", "Bob", "Charlie"))
            fail("InvalidSplitException expected")
        } catch (e: ExpenseDomainException.InvalidSplitException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `BR8 - calculateBalancesWithSplit accepts valid split that matches amount`() {
        val expenses = listOf(
            Expense(
                description = "Dinner",
                amount = 90.0,
                paidBy = "Alice",
                splitDetails = mapOf("Alice" to 30.0, "Bob" to 30.0, "Charlie" to 30.0)
            )
        )
        val result = ExpenseCalculator.calculateBalancesWithSplit(
            expenses,
            listOf("Alice", "Bob", "Charlie")
        )

        // Alice paid 90, owes 30 → net +60
        // Bob owes 30 → net -30
        // Charlie owes 30 → net -30
        val alice = result.first { it.participant == "Alice" }
        val bob = result.first { it.participant == "Bob" }
        val charlie = result.first { it.participant == "Charlie" }

        assertEquals(60.0, alice.amount, delta)
        assertEquals(-30.0, bob.amount, delta)
        assertEquals(-30.0, charlie.amount, delta)
    }

    @Test
    fun `BR8 - calculateBalancesWithSplit falls back to equal split when splitDetails is empty`() {
        val expenses = listOf(Expense(description = "Taxi", amount = 60.0, paidBy = "Alice"))
        val result = ExpenseCalculator.calculateBalancesWithSplit(
            expenses,
            listOf("Alice", "Bob", "Charlie")
        )

        val alice = result.first { it.participant == "Alice" }
        assertEquals(40.0, alice.amount, delta) // paid 60, owes 20 → +40
    }

    @Test
    fun `BR8 - validateSplit passes when split matches total within threshold`() {
        // Should not throw
        ExpenseCalculator.validateSplit(90.0, mapOf("A" to 45.0, "B" to 45.0))
    }

    @Test
    fun `BR8 - validateSplit throws when split is off by more than threshold`() {
        try {
            ExpenseCalculator.validateSplit(100.0, mapOf("A" to 40.0, "B" to 40.0))
            fail("InvalidSplitException expected")
        } catch (e: ExpenseDomainException.InvalidSplitException) {
            assertNotNull(e)
        }
    }

    @Test
    fun `BR8 - validateSplit passes for empty splitDetails`() {
        // Empty means no custom split — should pass silently
        ExpenseCalculator.validateSplit(100.0, emptyMap())
    }

    // ── calculateReimbursements ────────────────────────────────────────────────

    @Test
    fun `calculateReimbursements returns empty list when all balances are zero`() {
        val balances = listOf(
            Balance("Alice", 0.0),
            Balance("Bob", 0.0)
        )
        val result = ExpenseCalculator.calculateReimbursements(balances)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `BR6 - calculateReimbursements minimises transactions for simple case`() {
        // Alice +50, Bob -50 → exactly 1 transaction
        val balances = listOf(
            Balance("Alice", 50.0),
            Balance("Bob", -50.0)
        )
        val result = ExpenseCalculator.calculateReimbursements(balances)
        assertEquals(1, result.size)
        assertEquals("Bob", result[0].from)
        assertEquals("Alice", result[0].to)
        assertEquals(50.0, result[0].amount, delta)
    }

    @Test
    fun `BR6 - calculateReimbursements produces at most n-1 transactions for n participants`() {
        // 4 participants: worst case = 3 transactions
        val balances = listOf(
            Balance("Alice", 30.0),
            Balance("Bob", 20.0),
            Balance("Charlie", -25.0),
            Balance("Dave", -25.0)
        )
        val result = ExpenseCalculator.calculateReimbursements(balances)
        assertTrue("Expected at most 3 transactions, got ${result.size}", result.size <= 3)
    }

    @Test
    fun `BR6 - calculateReimbursements settles debts completely`() {
        val balances = listOf(
            Balance("Alice", 60.0),
            Balance("Bob", -30.0),
            Balance("Charlie", -30.0)
        )
        val result = ExpenseCalculator.calculateReimbursements(balances)

        val totalReimbursed = result.sumOf { it.amount }
        assertEquals(60.0, totalReimbursed, delta)
    }

    @Test
    fun `BR7 - calculateReimbursements from is always the debtor, to is always the creditor`() {
        val balances = listOf(
            Balance("Alice", 50.0), // creditor
            Balance("Bob", -50.0) // debtor
        )
        val result = ExpenseCalculator.calculateReimbursements(balances)
        assertEquals("Bob", result[0].from) // debtor pays
        assertEquals("Alice", result[0].to) // creditor receives
    }

    @Test
    fun `calculateReimbursements ignores balances below threshold`() {
        // 0.001 is below BALANCE_THRESHOLD (0.01) → should produce no transaction
        val balances = listOf(
            Balance("Alice", 0.001),
            Balance("Bob", -0.001)
        )
        val result = ExpenseCalculator.calculateReimbursements(balances)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateReimbursements handles three-way split correctly`() {
        val expenses = listOf(
            Expense(description = "Hotel", amount = 300.0, paidBy = "Alice"),
            Expense(description = "Food", amount = 90.0, paidBy = "Bob")
        )
        val participants = listOf("Alice", "Bob", "Charlie")
        val balances = ExpenseCalculator.calculateBalances(expenses, participants)
        val result = ExpenseCalculator.calculateReimbursements(balances)

        // Total = 390, share = 130 each
        // Alice paid 300 → +170
        // Bob paid 90 → -40
        // Charlie paid 0 → -130
        // Verify all debts are settled
        val paidByDebtor = result.groupBy { it.from }.mapValues { e -> e.value.sumOf { it.amount } }
        val balanceMap = balances.associateBy { it.participant }
        paidByDebtor.forEach { (debtor, paid) ->
            val debt = -(balanceMap[debtor]?.amount ?: 0.0)
            assertEquals(debt, paid, delta)
        }
    }

    // ── convertAmount (BR9) ───────────────────────────────────────────────────

    @Test
    fun `BR9 - convertAmount returns amount multiplied by rate`() {
        val result = ExpenseCalculator.convertAmount(100.0, 1.08)
        assertEquals(108.0, result!!, delta)
    }

    @Test
    fun `BR9 - convertAmount returns null when rate is null`() {
        assertNull(ExpenseCalculator.convertAmount(100.0, null))
    }

    @Test
    fun `BR9 - convertAmount returns null when rate is zero`() {
        assertNull(ExpenseCalculator.convertAmount(100.0, 0.0))
    }

    @Test
    fun `BR9 - convertAmount handles fractional rates correctly`() {
        // 100 EUR at 0.86 GBP/EUR = 86 GBP
        val result = ExpenseCalculator.convertAmount(100.0, 0.86)
        assertEquals(86.0, result!!, delta)
    }

    @Test
    fun `BR9 - convertAmount with rate 1 0 returns same amount`() {
        val result = ExpenseCalculator.convertAmount(250.0, 1.0)
        assertEquals(250.0, result!!, delta)
    }

    // ── CA5: edge cases ───────────────────────────────────────────────────────

    @Test
    fun `CA5 - calculateBalances with zero-amount expense keeps balances at zero`() {
        val expenses = listOf(Expense(description = "Free item", amount = 0.0, paidBy = "Alice"))
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob"))
        result.forEach { assertEquals(0.0, it.amount, delta) }
    }

    @Test
    fun `CA5 - calculateReimbursements with empty balances list returns empty`() {
        val result = ExpenseCalculator.calculateReimbursements(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `CA5 - convertAmount with very small amount returns correct result`() {
        val result = ExpenseCalculator.convertAmount(0.01, 1.08)
        assertEquals(0.0108, result!!, 0.00001)
    }

    @Test
    fun `CA5 - convertAmount with very large amount does not overflow`() {
        val result = ExpenseCalculator.convertAmount(1_000_000.0, 1.5)
        assertEquals(1_500_000.0, result!!, 1.0)
    }

    @Test
    fun `CA5 - calculateBalances handles participant who did not participate in any expense`() {
        val expenses = listOf(Expense(description = "Lunch", amount = 40.0, paidBy = "Alice"))
        val result = ExpenseCalculator.calculateBalances(expenses, listOf("Alice", "Bob", "Charlie"))

        val charlie = result.first { it.participant == "Charlie" }
        // Charlie paid nothing, owes 40/3
        assertTrue("Charlie should owe money", charlie.amount < 0)
    }
}
