package fr.univ.nantes.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.feature.expense.Expense
import fr.univ.nantes.feature.expense.GroupData
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot tests for HomeScreen and GroupDetailScreen.
 *
 * RG1: light theme.
 * CA1: covers home screen and group detail screen.
 * CA2: empty list, groups list, expenses tab, balances tab (with debts, all settled).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class HomePaparazziTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** CA2: home screen with no groups (empty state) */
    @Test
    fun homeScreen_emptyList() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                HomeScreen(modifier = Modifier.fillMaxSize(), viewModel = null)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA1 + CA2: home screen with groups (success state) */
    @Test
    fun homeScreen_withGroups() {
        val groups = listOf(
            GroupData(
                id = "1",
                groupName = "Summer Vacation 2025",
                participants = listOf("Alice", "Bob", "Charlie"),
                expenses = listOf(
                    Expense("1", "Hotel", 300.0, "Alice"),
                    Expense("2", "Restaurant", 80.0, "Bob")
                )
            ),
            GroupData(
                id = "2",
                groupName = "Ski Weekend",
                participants = listOf("Julie", "Marc"),
                expenses = listOf(Expense("3", "Chalet rental", 200.0, "Julie"))
            )
        )
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupsList(groups = groups, onGroupClick = {}, modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA1 + CA2: group detail screen -- expenses tab */
    @Test
    fun groupDetailScreen_expensesTab() {
        val group = GroupData(
            id = "1",
            groupName = "Summer Vacation 2025",
            participants = listOf("Alice", "Bob", "Charlie"),
            expenses = listOf(
                Expense("1", "Gas", 80.0, "Alice"),
                Expense("2", "Hotel", 300.0, "Bob"),
                Expense("3", "Restaurant", 120.0, "Charlie")
            )
        )
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupDetailScreen(group = group, onBack = {}, isLoggedIn = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: balances tab with debts */
    @Test
    fun groupDetailScreen_balancesTab_withDebts() {
        val group = GroupData(
            id = "2",
            groupName = "Ski Weekend",
            participants = listOf("Julie", "Marc", "Sophie"),
            expenses = listOf(
                Expense("1", "Chalet rental", 300.0, "Marc"),
                Expense("2", "Ski pass", 150.0, "Julie"),
                Expense("3", "Groceries", 60.0, "Sophie")
            )
        )
        val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance("EUR")
        }
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                BalancesTab(
                    group = group,
                    originalFormat = fmt,
                    userFormat = fmt,
                    showConversion = false,
                    convertAmount = { amount, _ -> amount }
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: balances tab -- all settled */
    @Test
    fun groupDetailScreen_balancesTab_allSettled() {
        val group = GroupData(
            id = "3",
            groupName = "Team Dinner",
            participants = listOf("Alice", "Bob"),
            expenses = listOf(
                Expense("1", "Restaurant", 60.0, "Alice"),
                Expense("2", "Dessert", 60.0, "Bob")
            )
        )
        val fmt = NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance("EUR")
        }
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                BalancesTab(
                    group = group,
                    originalFormat = fmt,
                    userFormat = fmt,
                    showConversion = false,
                    convertAmount = { amount, _ -> amount }
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
