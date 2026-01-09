package fr.univ.nantes.data.login.di

import fr.univ.nantes.data.login.LoginRepository
import fr.univ.nantes.data.login.LoginRepositoryMockImpl
import fr.univ.nantes.data.login.LoginUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val loginModule =
    module {
        factoryOf(::LoginUseCase)
        factoryOf(::LoginRepositoryMockImpl) bind LoginRepository::class
    }
