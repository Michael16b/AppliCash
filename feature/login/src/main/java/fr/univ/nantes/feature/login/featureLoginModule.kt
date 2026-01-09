package fr.univ.nantes.feature.login

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureLoginModule =
    module {
        viewModelOf(::LoginViewModel)
    }
