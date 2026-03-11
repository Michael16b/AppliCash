package fr.univ.nantes.data.currency.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExchangeRateDao {

    @Query("SELECT rate FROM exchange_rates WHERE base_currency = :base AND target_currency = :target LIMIT 1")
    suspend fun getRate(base: String, target: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates WHERE fetched_at < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT fetched_at FROM exchange_rates WHERE base_currency = :base LIMIT 1")
    suspend fun getLastFetchTime(base: String): Long?

    @Query("SELECT DISTINCT target_currency FROM exchange_rates WHERE base_currency = :base ORDER BY target_currency ASC")
    suspend fun getAvailableCurrencies(base: String): List<String>
}
