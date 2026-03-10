package fr.univ.nantes.feature.expense.di

import fr.univ.nantes.data.currency.CurrencyRepository
import fr.univ.nantes.data.expense.repository.ExpenseRepository
import fr.univ.nantes.domain.profil.ProfileUseCase
import fr.univ.nantes.feature.expense.ExpenseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureExpenseModule = module {
    viewModel {
        ExpenseViewModel(
            get<ExpenseRepository>(),
            get<ProfileUseCase>(),
            get<CurrencyRepository>()
        )
    }
}
