package fr.univ.nantes.feature.profil.di

import fr.univ.nantes.feature.profil.ProfilViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileFeatureModule = module {
    viewModelOf(::ProfilViewModel)
}
