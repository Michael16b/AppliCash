package fr.univ.nantes.domain.profil.di

import fr.univ.nantes.domain.profil.ProfileUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val profileDomainModule = module {
    singleOf(::ProfileUseCase)
}
