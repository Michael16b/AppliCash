package fr.univ.nantes.data.currency.db

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["base_currency", "target_currency"]
)
data class ExchangeRateEntity(
    @ColumnInfo(name = "base_currency") val baseCurrency: String,
    @ColumnInfo(name = "target_currency") val targetCurrency: String,
    @ColumnInfo(name = "rate") val rate: Double,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long
)

