package fr.univ.nantes.data.expense.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import fr.univ.nantes.data.expense.dao.ExpenseDao
import fr.univ.nantes.data.expense.dao.ExpenseGroupDao
import fr.univ.nantes.data.expense.dao.ParticipantDao
import fr.univ.nantes.data.expense.dto.ExpenseSnapshot
import fr.univ.nantes.data.expense.dto.GroupSnapshot
import fr.univ.nantes.data.expense.entity.ExpenseEntity
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.entity.ParticipantEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

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

sealed interface JoinGroupResult {
    data class Success(val groupId: String) : JoinGroupResult
    data object InvalidCode : JoinGroupResult
    data object MissingUserName : JoinGroupResult
    data object AlreadyMember : JoinGroupResult
}

interface ExpenseRepository {
    fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>>
    fun observeGroupWithDetails(groupId: String): Flow<GroupWithDetails?>
    suspend fun getGroupWithDetails(groupId: String): GroupWithDetails?
    suspend fun createGroup(groupName: String, participants: List<String>): String
    suspend fun addParticipantToGroup(groupId: String, participantName: String)
    suspend fun addExpenseToGroup(
        groupId: String,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: Int = 0,
        splitDetails: String = "{}"
    )
    suspend fun deleteGroup(groupId: String)
    suspend fun deleteExpense(expenseId: String)
    suspend fun updateGroupName(groupId: String, groupName: String)
    suspend fun removeParticipantFromGroup(groupId: String, participantName: String)
    suspend fun updateGroup(
        groupId: String,
        newName: String?,
        addParticipants: List<String>,
        removeParticipants: List<String>
    )
    suspend fun canViewShareCode(groupId: String, userName: String?): Boolean
    suspend fun joinGroupByShareCode(shareCode: String, userName: String?): JoinGroupResult
    suspend fun startRealtimeSync(id: String, viewModelScope: CoroutineScope)
    fun stopRealtimeSync()
    /** Effectue une synchronisation unique (one-shot) depuis Firebase pour un groupe donné. */
    suspend fun syncGroupFromFirebase(groupId: String)

}

class ExpenseRepositoryImpl(
    private val groupDao: ExpenseGroupDao,
    private val participantDao: ParticipantDao,
    private val expenseDao: ExpenseDao,
    private val firebaseDbProvider: () -> DatabaseReference = {
        FirebaseDatabase.getInstance().reference.child("shared_groups")
    }
) : ExpenseRepository {

    private val firebaseDb: DatabaseReference by lazy { firebaseDbProvider() }

    override fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>> {
        return groupDao.getAllGroupsWithDetails()
    }

    override fun observeGroupWithDetails(groupId: String): Flow<GroupWithDetails?> {
        return groupDao.observeGroupWithDetails(groupId)
    }

    override suspend fun getGroupWithDetails(groupId: String): GroupWithDetails? {
        return groupDao.getGroupWithDetails(groupId)
    }

    override suspend fun createGroup(groupName: String, participants: List<String>): String {
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
        val shareCode = generateUniqueShareCode()
        val groupEntity = ExpenseGroupEntity(groupName = groupName, shareCode = shareCode)
        groupDao.insertGroup(groupEntity)
        val groupId = groupEntity.id
        val participantEntities = participants.map { name ->
            ParticipantEntity(groupId = groupId, name = name)
        }

        participantDao.insertParticipants(participantEntities)

        val snapshot = GroupSnapshot(
            groupName = groupName,
            shareCode = shareCode,
            participants = participants
        )
        firebaseDb.child(shareCode).setValue(snapshot)
        return groupId
    }

    override suspend fun addParticipantToGroup(groupId: String, participantName: String) {
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
        syncParticipantsToFirebase(groupId)
    }

    override suspend fun addExpenseToGroup(
        groupId: String,
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
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            val allExpenses = expenseDao.getExpensesByGroupId(groupId).map {
                ExpenseSnapshot(it.description, it.amount, it.paidBy, it.splitType, it.splitDetails)
            }
            firebaseDb.child(group.shareCode).child("expenses").setValue(allExpenses)
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        val group = groupDao.getGroupById(groupId)
        groupDao.deleteGroup(groupId)
        if (group != null) {
            firebaseDb.child(group.shareCode).removeValue()
        }
    }

    override suspend fun deleteExpense(expenseId: String) {
        val expense = expenseDao.getExpenseById(expenseId)
        expenseDao.deleteExpense(expenseId)
        if (expense != null) {
            syncExpensesToFirebase(expense.groupId)
        }
    }

    override suspend fun updateGroupName(groupId: String, groupName: String) {
        groupDao.updateGroupName(groupId, groupName)
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            firebaseDb.child(group.shareCode).child("groupName").setValue(groupName)
        }
    }

    override suspend fun removeParticipantFromGroup(groupId: String, participantName: String) {
        // BR5: cannot remove a member who has expenses
        val expenses = expenseDao.getExpensesByGroupId(groupId)
        if (expenses.any { it.paidBy == participantName }) {
            throw ExpenseBusinessException.MemberHasExpensesException(participantName)
        }
        participantDao.deleteParticipantByName(groupId, participantName)
        syncParticipantsToFirebase(groupId)
    }

    override suspend fun updateGroup(
        groupId: String,
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
        if (newName != null) {
            val group = groupDao.getGroupById(groupId)
            if (group != null) {
                firebaseDb.child(group.shareCode).child("groupName").setValue(newName)
            }
        }
        syncParticipantsToFirebase(groupId)
    }

    override suspend fun joinGroupByShareCode(shareCode: String, userName: String?): JoinGroupResult {
        val normalizedCode = shareCode.trim().uppercase()
        val normalizedUser = userName?.trim().orEmpty()

        if (normalizedUser.isBlank()) return JoinGroupResult.MissingUserName

        try {
            val data = firebaseDb.child(normalizedCode).get().await()
            if (!data.exists()) return JoinGroupResult.InvalidCode

            val snapshot = data.getValue(GroupSnapshot::class.java) ?: return JoinGroupResult.InvalidCode

            val alreadyMember = snapshot.participants.any { it.equals(normalizedUser, ignoreCase = true) }

            val localGroupEntity = ExpenseGroupEntity(groupName = snapshot.groupName, shareCode = normalizedCode)
            groupDao.insertGroup(localGroupEntity)
            val localGroupId = localGroupEntity.id

            val updatedMembers = if (!alreadyMember) snapshot.participants + normalizedUser else snapshot.participants
            participantDao.insertParticipants(updatedMembers.map { ParticipantEntity(groupId = localGroupId, name = it) })

            val expensesToImport = snapshot.expenses.map {
                ExpenseEntity(
                    groupId = localGroupId,
                    description = it.description,
                    amount = it.amount,
                    paidBy = it.paidBy,
                    splitType = it.splitType,
                    splitDetails = it.splitDetails
                )
            }
            expenseDao.insertExpenses(expensesToImport)

            if (!alreadyMember) {
                firebaseDb.child(normalizedCode).child("participants").setValue(updatedMembers)
            }

            return JoinGroupResult.Success(localGroupId)
        } catch (_: Exception) {
            return JoinGroupResult.InvalidCode
        }
    }

    private suspend fun generateUniqueShareCode(length: Int = 6): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(20) {
            val candidate = buildString(length) {
                repeat(length) {
                    append(alphabet[Random.nextInt(alphabet.length)])
                }
            }
            if (groupDao.getGroupByShareCode(candidate) == null) {
                return candidate
            }
        }
        return buildString(length + 2) {
            repeat(length + 2) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }

    private var activeListener: ValueEventListener? = null
    private var activeGroupRef: DatabaseReference? = null

    override suspend fun startRealtimeSync(id: String, viewModelScope: CoroutineScope) {
        stopRealtimeSync()

        viewModelScope.launch {
            val group = groupDao.getGroupById(id) ?: return@launch
            activeGroupRef = firebaseDb.child(group.shareCode)

            activeListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteData = snapshot.getValue(GroupSnapshot::class.java) ?: return

                    viewModelScope.launch(Dispatchers.IO) {
                        groupDao.insertGroup(
                            group.copy(
                                groupName = remoteData.groupName,
                                shareCode = remoteData.shareCode.ifBlank { group.shareCode }
                            )
                        )

                        // Replace participants: wipe local list then re-insert from Firebase
                        participantDao.deleteAllParticipantsByGroupId(id)
                        val participants = remoteData.participants.map {
                            ParticipantEntity(groupId = id, name = it)
                        }
                        participantDao.insertParticipants(participants)

                        // Replace expenses: wipe local list then re-insert from Firebase
                        expenseDao.deleteAllExpensesByGroupId(id)
                        val expenses = remoteData.expenses.map {
                            ExpenseEntity(
                                groupId = id,
                                description = it.description,
                                amount = it.amount,
                                paidBy = it.paidBy,
                                splitType = it.splitType,
                                splitDetails = it.splitDetails
                            )
                        }
                        expenseDao.insertExpenses(expenses)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            activeGroupRef?.addValueEventListener(activeListener!!)
        }
    }

    override fun stopRealtimeSync() {
        activeListener?.let { activeGroupRef?.removeEventListener(it) }
        activeListener = null
        activeGroupRef = null
    }

    override suspend fun syncGroupFromFirebase(groupId: String) {
        val group = groupDao.getGroupById(groupId) ?: return
        try {
            val snapshot = firebaseDb.child(group.shareCode).get().await()
            val remoteData = snapshot.getValue(GroupSnapshot::class.java) ?: return

            groupDao.insertGroup(group.copy(groupName = remoteData.groupName))

            participantDao.deleteAllParticipantsByGroupId(groupId)
            participantDao.insertParticipants(
                remoteData.participants.map { ParticipantEntity(groupId = groupId, name = it) }
            )

            expenseDao.deleteAllExpensesByGroupId(groupId)
            expenseDao.insertExpenses(
                remoteData.expenses.map {
                    ExpenseEntity(
                        groupId = groupId,
                        description = it.description,
                        amount = it.amount,
                        paidBy = it.paidBy,
                        splitType = it.splitType,
                        splitDetails = it.splitDetails
                    )
                }
            )
        } catch (_: Exception) { }
    }

    override suspend fun canViewShareCode(groupId: String, userName: String?): Boolean {
        val normalizedUser = userName?.trim().orEmpty()
        if (normalizedUser.isBlank()) return false

        return  participantDao.isParticipantInGroup(groupId, normalizedUser)
    }

    private suspend fun syncParticipantsToFirebase(groupId: String) {
        val group = groupDao.getGroupById(groupId) ?: return
        val participants = participantDao.getParticipantsByGroupId(groupId).map { it.name }
        firebaseDb.child(group.shareCode).child("participants").setValue(participants)
    }

    private suspend fun syncExpensesToFirebase(groupId: String) {
        val group = groupDao.getGroupById(groupId) ?: return
        val allExpenses = expenseDao.getExpensesByGroupId(groupId).map {
            ExpenseSnapshot(it.description, it.amount, it.paidBy, it.splitType, it.splitDetails)
        }
        firebaseDb.child(group.shareCode).child("expenses").setValue(allExpenses)
    }
}
