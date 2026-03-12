package fr.univ.nantes.archi

import android.os.Bundle
import android.os.Environment
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.feature.expense.AddExpenseRoute
import fr.univ.nantes.feature.expense.AddExpenseScreen
import fr.univ.nantes.feature.expense.BalanceRoute
import fr.univ.nantes.feature.expense.BalanceScreen
import fr.univ.nantes.feature.expense.EditGroup
import fr.univ.nantes.feature.expense.EditGroupScreen
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
import java.io.File

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
                    viewModel = expenseViewModel
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
                                    navController.navigate(AddExpenseRoute(groupId = group.id))
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
                        onEditGroup = {
                            navController.navigate(EditGroup(groupId = group.id))
                        },
                        isLoggedIn = state.isLoggedIn,
                        onRequireLogin = {
                            navController.navigate(Login) {
                                popUpTo<Home> { inclusive = false }
                            }
                        },
                        userCurrencyCode = state.userCurrencyCode,
                        convertAmount = { amount, from -> expenseViewModel.convertAmount(amount, from) }
                    )
                }
            }
            composable<AddExpenseRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<AddExpenseRoute>()
                LaunchedEffect(route.groupId) {
                    expenseViewModel.loadGroup(route.groupId)
                }

                // Receipt photo state and launcher
                val context = androidx.compose.ui.platform.LocalContext.current
                val receiptPathState = remember { mutableStateOf<String?>(null) }
                val currentFile = remember { mutableStateOf<File?>(null) }
                val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                    if (success) {
                        currentFile.value?.let { file ->
                            receiptPathState.value = file.absolutePath
                        }
                    } else {
                        // failed or cancelled: delete temp file
                        currentFile.value?.delete()
                        currentFile.value = null
                        receiptPathState.value = null
                    }
                }

                val onStartCamera = {
                    try {
                        // create file in app external files Pictures/receipts
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val fileName = "RECEIPT_${route.groupId}_$timeStamp.jpg"
                        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        val receiptsDir = File(picturesDir, "receipts")
                        if (!receiptsDir.exists()) receiptsDir.mkdirs()
                        val file = File(receiptsDir, fileName)
                        file.createNewFile()
                        currentFile.value = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        takePictureLauncher.launch(uri)
                    } catch (_: Exception) {
                        // ignore for MVP
                    }
                }

                AddExpenseScreen(
                    viewModel = expenseViewModel,
                    navigateBack = { navController.popBackStack() },
                    onStartCamera = onStartCamera,
                    receiptPreviewPath = receiptPathState.value,
                    onClearReceipt = {
                        currentFile.value?.delete()
                        currentFile.value = null
                        receiptPathState.value = null
                    }
                )
            }
            composable<EditGroup> { backStackEntry ->
                val route = backStackEntry.toRoute<EditGroup>()
                EditGroupScreen(
                    groupId = route.groupId,
                    viewModel = expenseViewModel,
                    onBack = { navController.popBackStack() }
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
