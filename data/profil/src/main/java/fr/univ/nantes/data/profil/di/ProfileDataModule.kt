package fr.univ.nantes.data.profil.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.univ.nantes.data.profil.CurrencyEntity
import fr.univ.nantes.data.profil.ProfileDatabase
import fr.univ.nantes.data.profil.ProfileRepositoryImpl
import fr.univ.nantes.domain.profil.ProfileRepository
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val profileDataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ProfileDatabase::class.java,
            "profile.db"
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // prepopulate currency table synchronously to ensure data is available before first use
                    // This runs during database creation (first app launch only) so the blocking is acceptable
                    // Alternative async approach would require handling empty currency list throughout the app
                    runBlocking {
                        val dao = get<ProfileDatabase>().currencyDao()
                        dao.insertAll(
                            listOf(
                                CurrencyEntity(code = "EUR", name = "Euro"),
                                CurrencyEntity(code = "USD", name = "Dollar"),
                                CurrencyEntity(code = "GBP", name = "Livre"),
                                CurrencyEntity(code = "JPY", name = "Yen")
                            )
                        )
                    }
                }
            })
            .build()
    }
    single { get<ProfileDatabase>().profileDao() }
    single { get<ProfileDatabase>().currencyDao() }
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class
}
