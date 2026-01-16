package com.example.expense.di

import com.example.expense.ExpenseViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureExpenseModule = module {
    viewModel { ExpenseViewModel() }
}
