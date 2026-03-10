package fr.univ.nantes.domain.expense

/**
 * Pure domain exceptions for the expense feature.
 */
sealed class ExpenseDomainException(message: String) : Exception(message) {
    /** BR1 (same as data layer BR1) — kept here for use-case-level validation. */
    @Suppress("unused")
    class NotEnoughMembersException :
        ExpenseDomainException("A group must have at least 2 members (BR1)")

    /** BR2 — member name cannot be blank. */
    @Suppress("unused")
    class EmptyMemberNameException :
        ExpenseDomainException("A member name cannot be empty (BR2)")

    /** BR4 — expense amount must be > 0. */
    @Suppress("unused")
    class InvalidAmountException :
        ExpenseDomainException("The expense amount must be greater than 0 (BR4)")

    /** BR8 — split shares must sum to 100 % of the total expense. */
    class InvalidSplitException(detail: String) :
        ExpenseDomainException("Split details are invalid: $detail (BR8)")
}
