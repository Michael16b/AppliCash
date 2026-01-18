package fr.univ.nantes.feature.expense.di

import fr.univ.nantes.feature.expense.ExpenseViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureExpenseModule = module {
    viewModel { ExpenseViewModel() }
}
