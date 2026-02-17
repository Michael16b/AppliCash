package fr.univ.nantes.archi.di

import fr.univ.nantes.feature.expense.di.featureExpenseModule
import fr.univ.nantes.data.login.di.loginModule
import fr.univ.nantes.domain.login.di.loginDomainModule
import fr.univ.nantes.feature.login.featureLoginModule
import fr.univ.nantes.data.expense.di.dataExpenseModule
import fr.univ.nantes.data.profil.di.profileDataModule
import fr.univ.nantes.domain.profil.di.profileDomainModule
import fr.univ.nantes.feature.profil.di.profileFeatureModule
import org.koin.core.module.Module

val appModules: List<Module> =
    listOf(
        loginModule,
        loginDomainModule,
        featureLoginModule,
        featureExpenseModule,
        dataExpenseModule,
        profileDataModule,
        profileDomainModule,
        profileFeatureModule,
        // Add your Koin modules here, e.g., networkModule, featureXModule, CoreModule, etc.
    )
