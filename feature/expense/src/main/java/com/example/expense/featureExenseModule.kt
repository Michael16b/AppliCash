package com.example.expense

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureExpenseModule = module {
    viewModel { ExpenseViewModel() }
}