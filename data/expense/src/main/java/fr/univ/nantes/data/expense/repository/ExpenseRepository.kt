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
        paidBy: String
    )
    suspend fun deleteGroup(groupId: Long)
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
        // Créer le groupe
        val groupId = groupDao.insertGroup(
            ExpenseGroupEntity(groupName = groupName)
        )

        // Ajouter les participants
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
        paidBy: String
    ) {
        expenseDao.insertExpense(
            ExpenseEntity(
                groupId = groupId,
                description = description,
                amount = amount,
                paidBy = paidBy
            )
        )
    }

    override suspend fun deleteGroup(groupId: Long) {
        groupDao.deleteGroup(groupId)
    }
}

