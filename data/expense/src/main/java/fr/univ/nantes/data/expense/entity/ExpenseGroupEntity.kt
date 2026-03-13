package fr.univ.nantes.data.expense.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "expense_groups",
    indices = [Index(value = ["shareCode"], unique = true)]
)
data class ExpenseGroupEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val groupName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val shareCode: String

    )

