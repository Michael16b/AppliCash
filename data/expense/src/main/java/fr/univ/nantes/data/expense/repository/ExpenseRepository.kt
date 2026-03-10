package fr.univ.nantes.data.expense.repository

import fr.univ.nantes.data.expense.dao.ExpenseDao
import fr.univ.nantes.data.expense.dao.ExpenseGroupDao
import fr.univ.nantes.data.expense.dao.ParticipantDao
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.flow.Flow

// ── Business exceptions ────────────────────────────────────────────────────────
sealed class ExpenseBusinessException(message: String) : Exception(message) {
    /** BR1: a group must have at least 2 members. */
    class NotEnoughMembersException :
        ExpenseBusinessException("A group must have at least 2 members (BR1)")

    /** BR2: a member name cannot be empty. */
    class EmptyMemberNameException :
        ExpenseBusinessException("A member name cannot be empty (BR2)")

    /** BR3: member names must be unique within a group. */
    class DuplicateMemberNameException(name: String) :
        ExpenseBusinessException("The name '$name' is already used in this group (BR3)")

    /** BR4: an expense requires an amount > 0. */
    class InvalidAmountException :
        ExpenseBusinessException("The expense amount must be greater than 0 (BR4)")

    /** BR5: a member with expenses cannot be removed. */
    class MemberHasExpensesException(name: String) :
        ExpenseBusinessException("Member '$name' cannot be removed because they have expenses (BR5)")
}

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
        // BR1: at least 2 members
        if (participants.size < 2) throw ExpenseBusinessException.NotEnoughMembersException()

        // BR2: no blank names
        participants.forEach { name ->
            if (name.isBlank()) throw ExpenseBusinessException.EmptyMemberNameException()
        }

        // BR3: unique names
        val seen = mutableSetOf<String>()
        participants.forEach { name ->
            if (!seen.add(name.trim())) throw ExpenseBusinessException.DuplicateMemberNameException(name)
        }

        val groupId = groupDao.insertGroup(
            ExpenseGroupEntity(groupName = groupName)
        )
        val participantEntities = participants.map { name ->
            ParticipantEntity(groupId = groupId, name = name)
        }
        participantDao.insertParticipants(participantEntities)
        return groupId
    }

    override suspend fun addParticipantToGroup(groupId: Long, participantName: String) {
        // BR2: name must not be blank
        if (participantName.isBlank()) throw ExpenseBusinessException.EmptyMemberNameException()

        // BR3: name must be unique within the group
        val existing = participantDao.getParticipantsByGroupId(groupId)
        if (existing.any { it.name == participantName.trim() }) {
            throw ExpenseBusinessException.DuplicateMemberNameException(participantName)
        }

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
        // BR4: amount must be > 0
        if (amount <= 0.0) throw ExpenseBusinessException.InvalidAmountException()

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
        // BR5: cannot remove a member who has expenses
        val expenses = expenseDao.getExpensesByGroupId(groupId)
        if (expenses.any { it.paidBy == participantName }) {
            throw ExpenseBusinessException.MemberHasExpensesException(participantName)
        }
        participantDao.deleteParticipantByName(groupId, participantName)
    }

    override suspend fun updateGroup(
        groupId: Long,
        newName: String?,
        addParticipants: List<String>,
        removeParticipants: List<String>
    ) {
        // BR2: new member names must not be blank
        addParticipants.forEach { name ->
            if (name.isBlank()) throw ExpenseBusinessException.EmptyMemberNameException()
        }

        // BR3: no duplicates among additions
        val seen = mutableSetOf<String>()
        addParticipants.forEach { name ->
            if (!seen.add(name.trim())) throw ExpenseBusinessException.DuplicateMemberNameException(name)
        }

        // BR5: verify that members to remove have no expenses
        if (removeParticipants.isNotEmpty()) {
            val expenses = expenseDao.getExpensesByGroupId(groupId)
            removeParticipants.forEach { name ->
                if (expenses.any { it.paidBy == name }) {
                    throw ExpenseBusinessException.MemberHasExpensesException(name)
                }
            }
        }

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
