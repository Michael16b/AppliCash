package fr.univ.nantes.data.login.di

import fr.univ.nantes.data.login.LoginRepositoryMockImpl
import fr.univ.nantes.domain.login.LoginRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val loginModule =
    module {
        factoryOf(::LoginRepositoryMockImpl) bind LoginRepository::class
    }
