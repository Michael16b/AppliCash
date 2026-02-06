package fr.univ.nantes.feature.expense.di

import fr.univ.nantes.feature.expense.ExpenseViewModel
import org.koin.core.module.dsl.*
import org.koin.dsl.module

val featureExpenseModule = module {
    viewModel { ExpenseViewModel() }
}
