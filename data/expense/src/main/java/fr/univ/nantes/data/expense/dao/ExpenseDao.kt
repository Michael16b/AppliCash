package fr.univ.nantes.data.expense.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.univ.nantes.data.expense.entity.ExpenseEntity

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getExpensesByGroupId(groupId: Long): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Long)
}
