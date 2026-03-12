package fr.univ.nantes.data.expense.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["groupId", "name"], unique = true)
    ]
)
data class ParticipantEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val name: String
)

