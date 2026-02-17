package fr.univ.nantes.data.login.di

import fr.univ.nantes.data.login.LoginRepositoryRoomImpl
import fr.univ.nantes.domain.login.LoginRepository
import org.koin.dsl.module

val loginModule = module {
    single<LoginRepository> { LoginRepositoryRoomImpl(get()) }
}
