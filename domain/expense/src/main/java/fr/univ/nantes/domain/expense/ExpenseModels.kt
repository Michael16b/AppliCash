package fr.univ.nantes.domain.expense

/**
 * Domain model representing an expense entry within a group.
 *
 * Note: Double is used for amounts. For production financial apps, prefer BigDecimal
 * or storing as Long (smallest currency unit, e.g. cents).
 */
@Suppress("unused")
data class Expense(
    val id: String = "",
    val description: String,
    val amount: Double,
    val paidBy: String,
    val splitType: SplitType = SplitType.EQUALLY,
    /** Maps participant name → share/amount depending on [splitType]. */
    val splitDetails: Map<String, Double> = emptyMap()
)

@Suppress("unused")
enum class SplitType(val code: Int) {
    EQUALLY(0),
    BY_SHARE(1),
    BY_AMOUNT(2);

    companion object {
        @Suppress("unused")
        fun fromCode(code: Int): SplitType = entries.firstOrNull { it.code == code } ?: EQUALLY
    }
}

/** Domain model representing a group of participants sharing expenses. */
@Suppress("unused")
data class GroupData(
    val id: String = "",
    val groupName: String = "",
    val participants: List<String> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

/**
 * Represents a participant's net balance within the group.
 *
 * BR7: positive amount = credit (others owe this person),
 *      negative amount = debt  (this person owes others).
 */
@Suppress("unused")
data class Balance(
    val participant: String,
    val amount: Double
)

/**
 * Represents a single reimbursement transaction.
 * [from] owes [amount] to [to].
 */
@Suppress("unused")
data class Reimbursement(
    val from: String,
    val to: String,
    val amount: Double
)
