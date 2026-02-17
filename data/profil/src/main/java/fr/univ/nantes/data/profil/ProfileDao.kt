package fr.univ.nantes.data.profil

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE is_logged_in = 1 LIMIT 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE profile SET is_logged_in = 0")
    suspend fun logout()

    @Query("SELECT COUNT(*) FROM profile WHERE is_logged_in = 1")
    suspend fun loggedInCount(): Int

    @Query("DELETE FROM profile")
    suspend fun clear()
}
