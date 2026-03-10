package fr.univ.nantes.data.expense.repository

import fr.univ.nantes.data.expense.dao.ExpenseDao
import fr.univ.nantes.data.expense.dao.ExpenseGroupDao
import fr.univ.nantes.data.expense.dao.ParticipantDao
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>>
    suspend fun getGroupWithDetails(groupId: Long): GroupWithDetails?
    suspend fun createGroup(groupName: String, participants: List<String>): Long
    suspend fun addParticipantToGroup(groupId: Long, participantName: String)
    suspend fun addExpenseToGroup(
        groupId: Long,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: Int = 0,
        splitDetails: String = "{}"
    )
    suspend fun deleteGroup(groupId: Long)
    suspend fun deleteExpense(expenseId: Long)
    suspend fun updateGroupName(groupId: Long, groupName: String)
    suspend fun removeParticipantFromGroup(groupId: Long, participantName: String)
    suspend fun updateGroup(
        groupId: Long,
        newName: String?,
        addParticipants: List<String>,
        removeParticipants: List<String>
    )
}

class ExpenseRepositoryImpl(
    private val groupDao: ExpenseGroupDao,
    private val participantDao: ParticipantDao,
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>> {
        return groupDao.getAllGroupsWithDetails()
    }

    override suspend fun getGroupWithDetails(groupId: Long): GroupWithDetails? {
        return groupDao.getGroupWithDetails(groupId)
    }

    override suspend fun createGroup(groupName: String, participants: List<String>): Long {
        val groupId = groupDao.insertGroup(
            ExpenseGroupEntity(groupName = groupName)
        )

        if (participants.isNotEmpty()) {
            val participantEntities = participants.map { name ->
                ParticipantEntity(groupId = groupId, name = name)
            }
            participantDao.insertParticipants(participantEntities)
        }

        return groupId
    }

    override suspend fun addParticipantToGroup(groupId: Long, participantName: String) {
        participantDao.insertParticipant(
            ParticipantEntity(groupId = groupId, name = participantName)
        )
    }

    override suspend fun addExpenseToGroup(
        groupId: Long,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: Int,
        splitDetails: String
    ) {
        expenseDao.insertExpense(
            ExpenseEntity(
                groupId = groupId,
                description = description,
                amount = amount,
                paidBy = paidBy,
                splitType = splitType,
                splitDetails = splitDetails
            )
        )
    }

    override suspend fun deleteGroup(groupId: Long) {
        groupDao.deleteGroup(groupId)
    }

    override suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpense(expenseId)
    }

    override suspend fun updateGroupName(groupId: Long, groupName: String) {
        groupDao.updateGroupName(groupId, groupName)
    }

    override suspend fun removeParticipantFromGroup(groupId: Long, participantName: String) {
        participantDao.deleteParticipantByName(groupId, participantName)
    }

    override suspend fun updateGroup(
        groupId: Long,
        newName: String?,
        addParticipants: List<String>,
        removeParticipants: List<String>
    ) {
        if (newName != null) {
            groupDao.updateGroupName(groupId, newName)
        }
        participantDao.updateParticipants(
            groupId = groupId,
            addParticipants = addParticipants.map { ParticipantEntity(groupId = groupId, name = it) },
            removeNames = removeParticipants
        )
    }

}

