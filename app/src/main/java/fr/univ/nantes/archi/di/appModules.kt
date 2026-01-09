package fr.univ.nantes.archi.di

import fr.univ.nantes.data.login.di.loginModule
import fr.univ.nantes.domain.login.di.loginDomainModule
import fr.univ.nantes.feature.login.featureLoginModule
import org.koin.core.module.Module

val appModules: List<Module> =
    listOf(
        loginModule,
        loginDomainModule,
        featureLoginModule,
        // Add your Koin modules here, e.g., networkModule, featureXModule, CoreModule, etc.
    )
