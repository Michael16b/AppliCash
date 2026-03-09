package fr.univ.nantes.archi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.univ.nantes.core.ui.AppliCashTheme
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
import fr.univ.nantes.home.GroupDetail
import fr.univ.nantes.home.GroupDetailScreen
import fr.univ.nantes.home.Home
import fr.univ.nantes.home.HomeScreen
import kotlinx.coroutines.launch
import fr.univ.nantes.domain.profil.ProfileUseCase
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AppliCashTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Splash,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Splash> {
                SplashScreen(navigateNext = {
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
                        navController.navigate(Group)
                    },
                    onGroupClick = { groupData ->
                        navController.navigate(GroupDetail(groupId = groupData.id))
                    },
                    onProfileClick = {
                        scope.launch {
                            val loggedIn = runCatching { profileUseCase.isLoggedIn() }.getOrDefault(false)
                            if (loggedIn) {
                                navController.navigate(ProfilRoute)
                            } else {
                                navController.navigate(Login) {
                                    popUpTo<Home> { inclusive = false }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            composable<GroupDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<GroupDetail>()
                val state by expenseViewModel.state.collectAsState()
                val group = state.groups.find { it.id == route.groupId }
                if (group != null) {
                    GroupDetailScreen(
                        group = group,
                        onBack = { navController.popBackStack() },
                        onAddExpense = {
                            scope.launch {
                                val loggedIn = runCatching { profileUseCase.isLoggedIn() }.getOrDefault(false)
                                if (loggedIn) {
                                    expenseViewModel.loadGroup(group.id)
                                    navController.navigate(ExpenseRoute)
                                } else {
                                    navController.navigate(Login) {
                                        popUpTo<Home> { inclusive = false }
                                    }
                                }
                            }
                        },
                        onDeleteExpense = { expenseId ->
                            expenseViewModel.deleteExpense(expenseId, group.id)
                        },
                        isLoggedIn = state.isLoggedIn,
                        onRequireLogin = {
                            navController.navigate(Login) {
                                popUpTo<Home> { inclusive = false }
                            }
                        }
                    )
                }
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
