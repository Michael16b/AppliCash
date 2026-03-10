package fr.univ.nantes.data.currency.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Suppress("unused") // Instantiated via Koin using Room.databaseBuilder
@Database(entities = [ExchangeRateEntity::class], version = 1, exportSchema = false)
abstract class ExchangeRateDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao
}


