package fr.univ.nantes.data.profil.di

import androidx.room.Room
import fr.univ.nantes.data.profil.ProfileDatabase
import fr.univ.nantes.data.profil.ProfileRepositoryImpl
import fr.univ.nantes.domain.profil.ProfileRepository
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
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<ProfileDatabase>().profileDao() }
    single { get<ProfileDatabase>().currencyDao() }
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class
}
