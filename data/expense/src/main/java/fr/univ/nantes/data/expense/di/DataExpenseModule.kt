package fr.univ.nantes.data.expense.di

import androidx.room.Room
import fr.univ.nantes.data.expense.database.AppDatabase
import fr.univ.nantes.data.expense.repository.ExpenseRepository
import fr.univ.nantes.data.expense.repository.ExpenseRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataExpenseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "expense_database"
        ).build()
    }

    single { get<AppDatabase>().expenseGroupDao() }
    single { get<AppDatabase>().participantDao() }
    single { get<AppDatabase>().expenseDao() }

    single<ExpenseRepository> {
        ExpenseRepositoryImpl(
            groupDao = get(),
            participantDao = get(),
            expenseDao = get()
        )
    }
}
