package fr.univ.nantes.data.currency.di

import androidx.room.Room
import fr.univ.nantes.data.currency.CurrencyRepository
import fr.univ.nantes.data.currency.api.FrankfurterApi
import fr.univ.nantes.data.currency.db.ExchangeRateDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val currencyDataModule = module {
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            ExchangeRateDatabase::class.java,
            "exchange_rates.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { get<ExchangeRateDatabase>().exchangeRateDao() }

    single { FrankfurterApi(get()) }

    single { CurrencyRepository(get(), get()) }
}

