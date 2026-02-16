package fr.univ.nantes.data.expense.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.univ.nantes.data.expense.entity.ParticipantEntity

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Query("SELECT * FROM participants WHERE groupId = :groupId")
    suspend fun getParticipantsByGroupId(groupId: Long): List<ParticipantEntity>

    @Query("DELETE FROM participants WHERE id = :participantId")
    suspend fun deleteParticipant(participantId: Long)
}

