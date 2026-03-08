package fr.univ.nantes.data.expense.model

import androidx.room.Embedded
import androidx.room.Relation
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity

data class GroupWithDetails(
    @Embedded val group: ExpenseGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val participants: List<ParticipantEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val expenses: List<ExpenseEntity>
)
