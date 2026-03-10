package fr.univ.nantes.feature.expense

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.univ.nantes.core.ui.AppliCashTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Tests snapshot pour les écrans expense.
 * RG1 : thème clair. RG2 : composants testés en isolation. CA1+CA2 : tous les états.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ExpensePaparazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    // CA1 + CA2 : écran balance avec dettes
    @Test
    fun balanceContent_avecDettes() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                BalanceContent(
                    groupName = "Vacances à Nantes",
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

    // CA2 : balance — tout réglé
    @Test
    fun balanceContent_toutRegle() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                BalanceContent(
                    groupName = "Dîner Pizza",
                    balances = listOf(Balance("Alice", 0.0), Balance("Bob", 0.0)),
                    reimbursements = emptyList(),
                    userCurrencyCode = "EUR"
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // RG2 : composant MemberBalanceCard — créancier
    @Test
    fun memberBalanceCard_creancier() {
        val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
            currency = Currency.getInstance("EUR")
        }
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                MemberBalanceCard(balance = Balance("Alice", 45.50), formatter = formatter)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // RG2 : composant MemberBalanceCard — débiteur
    @Test
    fun memberBalanceCard_debiteur() {
        val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
            currency = Currency.getInstance("EUR")
        }
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                MemberBalanceCard(balance = Balance("Bob", -20.00), formatter = formatter)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA1 + CA2 : formulaire création de groupe — vide
    @Test
    fun groupScreenContent_formulaireVide() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupScreenContent(groupName = "", modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA2 : formulaire création de groupe — avec nom pré-rempli
    @Test
    fun groupScreenContent_avecNomPreRempli() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupScreenContent(
                    groupName = "Vacances été 2025",
                    currentUserName = "Alice",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
