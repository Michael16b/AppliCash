package fr.univ.nantes.feature.expense

//import org.koin.core.module.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureExpenseModule = module {
    viewModel { ExpenseViewModel() }
}
