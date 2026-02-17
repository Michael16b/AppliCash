package fr.univ.nantes.domain.profil.di

import fr.univ.nantes.domain.profil.ProfileUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val profileDomainModule = module {
    factoryOf(::ProfileUseCase)
}

