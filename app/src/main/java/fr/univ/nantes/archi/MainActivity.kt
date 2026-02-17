package fr.univ.nantes.archi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.domain.profil.ProfileUseCase
import fr.univ.nantes.feature.expense.BalanceRoute
import fr.univ.nantes.feature.expense.BalanceScreen
import fr.univ.nantes.feature.expense.ExpenseRoute
import fr.univ.nantes.feature.expense.ExpenseScreen
import fr.univ.nantes.feature.expense.ExpenseViewModel
import fr.univ.nantes.feature.expense.Group
import fr.univ.nantes.feature.expense.GroupScreen
import fr.univ.nantes.feature.login.Login
import fr.univ.nantes.feature.login.LoginScreen
import fr.univ.nantes.feature.profil.ProfilRoute
import fr.univ.nantes.feature.profil.ProfileScreen
import fr.univ.nantes.feature.splashscreen.Splash
import fr.univ.nantes.feature.splashscreen.SplashScreen
import fr.univ.nantes.home.Home
import fr.univ.nantes.home.HomeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    val profileUseCase: ProfileUseCase = koinInject()
    val scope = rememberCoroutineScope()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home(),
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Splash> {
                SplashScreen(navigateNext = {
                    // TODO: Check authentication state and route to Login if not authenticated
                    // For now, navigating directly to Home as authentication is not implemented
                    navController.navigate(Home()) {
                        popUpTo<Splash> { inclusive = true }
                    }
                })
            }
            composable<Login> {
                LoginScreen(
                    navigateToHome = { username ->
                        navController.navigate(Home(username = username)) {
                            popUpTo<Login> { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable<Group> {
                GroupScreen(
                    viewModel = expenseViewModel,
                    navigateToHome = {
                        navController.navigate(Home()) {
                            popUpTo<Group> { inclusive = true }
                        }
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
                        // Reset the ViewModel to ensure users start with a clean state
                        expenseViewModel.reset()
                        navController.navigate(Group) {
                            popUpTo<Group> { inclusive = true }
                        }
                    }
                )
            }
            composable<Home> {
                HomeScreen(
                    viewModel = expenseViewModel,
                    onAddGroupClick = {
                        scope.launch {
                            val loggedIn = profileUseCase.observeProfile().first() != null
                            if (loggedIn) {
                                navController.navigate(Group)
                            } else {
                                navController.navigate(Login)
                            }
                        }
                    },
                    onGroupClick = { groupData ->
                        expenseViewModel.loadGroup(groupData.id)
                        navController.navigate(ExpenseRoute)
                    },
                    onProfileClick = {
                        scope.launch {
                            val loggedIn = profileUseCase.observeProfile().first() != null
                            if (loggedIn) {
                                navController.navigate(ProfilRoute)
                            } else {
                                navController.navigate(Login)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable<ProfilRoute> {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Login) {
                            popUpTo<Home> { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
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
