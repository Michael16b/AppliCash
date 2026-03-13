package fr.univ.nantes.data.expense.dto

data class GroupSnapshot(
    val groupName: String = "",
    val shareCode: String = "",
    val participants: List<String> = emptyList(),
    val expenses: List<ExpenseSnapshot> = emptyList()
)

