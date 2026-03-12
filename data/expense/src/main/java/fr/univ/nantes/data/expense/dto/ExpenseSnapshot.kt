package fr.univ.nantes.data.expense.dto

data class ExpenseSnapshot(
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitType: Int = 0,
    val splitDetails: String = "{}"
)

