package fr.univ.nantes.data.expense.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import fr.univ.nantes.data.expense.entity.ExpenseGroupEntity
import fr.univ.nantes.data.expense.model.GroupWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseGroupDao {
    @Transaction
    @Query("SELECT * FROM expense_groups ORDER BY createdAt DESC")
    fun getAllGroupsWithDetails(): Flow<List<GroupWithDetails>>

    @Transaction
    @Query("SELECT * FROM expense_groups WHERE id = :groupId")
    suspend fun getGroupWithDetails(groupId: Long): GroupWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ExpenseGroupEntity): Long

    @Query("DELETE FROM expense_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query("SELECT * FROM expense_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): ExpenseGroupEntity?

    @Query("UPDATE expense_groups SET groupName = :groupName WHERE id = :groupId")
    suspend fun updateGroupName(groupId: Long, groupName: String)

    @Transaction
    @Query("SELECT * FROM expense_groups WHERE shareCode = :shareCode")
    suspend fun getGroupWithDetailsByShareCode(shareCode: String): GroupWithDetails?

    @Query("SELECT * FROM expense_groups WHERE shareCode = :shareCode")
    suspend fun getGroupByShareCode(shareCode: String): ExpenseGroupEntity?
}

