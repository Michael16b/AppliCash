package fr.univ.nantes.data.expense.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_groups")
data class ExpenseGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupName: String,
    val createdAt: Long = System.currentTimeMillis()
)
