package fr.univ.nantes.domain.login.di

import fr.univ.nantes.domain.login.LoginUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val loginDomainModule = module {
    factoryOf(::LoginUseCase)
}