package fr.univ.nantes.data.expense.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val description: String,
    val amount: Double,
    val paidBy: String,
    val splitType: Int = 0, // 0=Equally, 1=By share, 2=By amount
    val splitDetails: String = "{}", // JSON format: {"participant": amount}
    val createdAt: Long = System.currentTimeMillis()
)

