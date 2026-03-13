package fr.univ.nantes.data.expense.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import fr.univ.nantes.data.expense.entity.ParticipantEntity

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Query("SELECT * FROM participants WHERE groupId = :groupId")
    suspend fun getParticipantsByGroupId(groupId: String): List<ParticipantEntity>

    @Query("DELETE FROM participants WHERE id = :participantId")
    suspend fun deleteParticipant(participantId: String)

    @Query("DELETE FROM participants WHERE groupId = :groupId AND name = :participantName")
    suspend fun deleteParticipantByName(groupId: String, participantName: String)

    @Query("DELETE FROM participants WHERE groupId = :groupId")
    suspend fun deleteAllParticipantsByGroupId(groupId: String)

    @Transaction
    suspend fun updateParticipants(
        groupId: String,
        addParticipants: List<ParticipantEntity>,
        removeNames: List<String>
    ) {
        if (addParticipants.isNotEmpty()) {
            insertParticipants(addParticipants)
        }
        removeNames.forEach { deleteParticipantByName(groupId, it) }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM participants WHERE groupId = :groupId AND name = :participantName)")
    suspend fun isParticipantInGroup(groupId: String, participantName: String): Boolean
}

