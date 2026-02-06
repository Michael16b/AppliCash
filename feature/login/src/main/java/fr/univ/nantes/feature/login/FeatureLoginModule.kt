package fr.univ.nantes.feature.login

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureLoginModule =
    module {
        viewModel { LoginViewModel(get()) }
    }
