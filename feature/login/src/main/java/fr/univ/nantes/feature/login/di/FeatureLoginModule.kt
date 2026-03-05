package fr.univ.nantes.feature.login.di

import fr.univ.nantes.feature.login.LoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureLoginModule = module {
    viewModelOf(::LoginViewModel)
}
