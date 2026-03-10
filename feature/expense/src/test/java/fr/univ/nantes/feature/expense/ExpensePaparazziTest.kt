package fr.univ.nantes.feature.expense

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import fr.univ.nantes.core.ui.AppliCashTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Snapshot tests for the expense module screens.
 *
 * RG1: light theme.
 * RG2: reusable components tested in isolation (BalanceContent, MemberBalanceCard).
 * CA1: covers balance screen and group creation screen.
 * CA2: with debts, all settled, creditor card, debtor card, empty form, pre-filled form.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ExpensePaparazziTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** CA1 + CA2: balance screen with debts */
    @Test
    fun balanceContent_withDebts() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                BalanceContent(
                    groupName = "Vacation",
                    balances = listOf(
                        Balance("Alice", 25.50),
                        Balance("Bob", -10.00),
                        Balance("Charlie", -15.50)
                    ),
                    reimbursements = listOf(
                        Reimbursement("Bob", "Alice", 10.00),
                        Reimbursement("Charlie", "Alice", 15.50)
                    ),
                    userCurrencyCode = "EUR"
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: balance screen -- all settled */
    @Test
    fun balanceContent_allSettled() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                BalanceContent(
                    groupName = "Pizza Dinner",
                    balances = listOf(Balance("Alice", 0.0), Balance("Bob", 0.0)),
                    reimbursements = emptyList(),
                    userCurrencyCode = "EUR"
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** RG2: MemberBalanceCard component -- creditor */
    @Test
    fun memberBalanceCard_creditor() {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance("EUR")
        }
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                MemberBalanceCard(balance = Balance("Alice", 45.50), formatter = formatter)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** RG2: MemberBalanceCard component -- debtor */
    @Test
    fun memberBalanceCard_debtor() {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance("EUR")
        }
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                MemberBalanceCard(balance = Balance("Bob", -20.00), formatter = formatter)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA1 + CA2: group creation form -- empty */
    @Test
    fun groupScreenContent_emptyForm() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupScreenContent(groupName = "", modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: group creation form -- pre-filled */
    @Test
    fun groupScreenContent_preFilledForm() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupScreenContent(
                    groupName = "Summer Vacation 2025",
                    currentUserName = "Alice",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
