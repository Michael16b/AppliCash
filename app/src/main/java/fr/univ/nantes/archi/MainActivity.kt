package fr.univ.nantes.archi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.univ.nantes.feature.expense.BalanceRoute
import fr.univ.nantes.feature.expense.BalanceScreen
import fr.univ.nantes.feature.expense.ExpenseRoute
import fr.univ.nantes.feature.expense.ExpenseScreen
import fr.univ.nantes.feature.expense.ExpenseViewModel
import fr.univ.nantes.feature.expense.Group
import fr.univ.nantes.feature.expense.GroupScreen
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.feature.login.Login
import fr.univ.nantes.feature.login.LoginScreen
import fr.univ.nantes.home.Home
import fr.univ.nantes.home.HomeScreen
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppliCashTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val navController = rememberNavController()
    val expenseViewModel: ExpenseViewModel = koinViewModel()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Login,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Login> {
                LoginScreen(
                    navigateToHome = { username ->
                        navController.navigate(Home(username))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable<Group> {
                GroupScreen(
                    viewModel = expenseViewModel,
                    navigateToExpense = {
                        navController.navigate(ExpenseRoute)
                    }
                )
            }
            composable<ExpenseRoute> {
                ExpenseScreen(
                    viewModel = expenseViewModel,
                    navigateToBalance = {
                        navController.navigate(BalanceRoute)
                    }
                )
            }
            composable<BalanceRoute> {
                BalanceScreen(
                    viewModel = expenseViewModel,
                    navigateToGroup = {
                        navController.navigate(Group) {
                            popUpTo<Group> { inclusive = true }
                        }
                    }
                )
            }
            composable<Home> { backStackEntry ->
                val home: Home = backStackEntry.toRoute()
                HomeScreen(
                    name = home.username,
                    navigateToExpense = {
                        navController.navigate(Group)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppliCashTheme {
        App()
    }
}
