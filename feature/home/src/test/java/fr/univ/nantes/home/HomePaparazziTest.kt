package fr.univ.nantes.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.feature.expense.Expense
import fr.univ.nantes.feature.expense.GroupData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Tests snapshot pour HomeScreen et GroupDetailScreen.
 * RG1 : thème clair. CA1 : couverture accueil + détail. CA2 : liste vide, groupes, dépenses, soldes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class HomePaparazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    // CA2 : accueil sans groupe (état vide)
    @Test
    fun homeScreen_listeVide() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                HomeScreen(modifier = Modifier.fillMaxSize(), viewModel = null)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA1 + CA2 : accueil avec groupes (état succès)
    @Test
    fun homeScreen_avecGroupes() {
        val groups = listOf(
            GroupData(
                id = 1L,
                groupName = "Vacances été 2025",
                participants = listOf("Alice", "Bob", "Charlie"),
                expenses = listOf(
                    Expense(1, "Hôtel", 300.0, "Alice"),
                    Expense(2, "Restaurant", 80.0, "Bob")
                )
            ),
            GroupData(
                id = 2L,
                groupName = "Week-end ski",
                participants = listOf("Julie", "Marc"),
                expenses = listOf(Expense(3, "Location chalet", 200.0, "Julie"))
            )
        )
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupsList(groups = groups, onGroupClick = {}, modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA1 + CA2 : détail groupe — onglet dépenses
    @Test
    fun groupDetailScreen_ongletDepenses() {
        val group = GroupData(
            id = 1L,
            groupName = "Vacances été 2025",
            participants = listOf("Alice", "Bob", "Charlie"),
            expenses = listOf(
                Expense(1, "Essence", 80.0, "Alice"),
                Expense(2, "Hôtel", 300.0, "Bob"),
                Expense(3, "Restaurant", 120.0, "Charlie")
            )
        )
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                GroupDetailScreen(group = group, onBack = {}, isLoggedIn = true)
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA2 : onglet soldes avec dettes
    @Test
    fun groupDetailScreen_ongletSoldes_avecDettes() {
        val group = GroupData(
            id = 2L,
            groupName = "Week-end ski",
            participants = listOf("Julie", "Marc", "Sophie"),
            expenses = listOf(
                Expense(1, "Location chalet", 300.0, "Marc"),
                Expense(2, "Forfait ski", 150.0, "Julie"),
                Expense(3, "Courses", 60.0, "Sophie")
            )
        )
        val fmt = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
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

    // CA2 : onglet soldes — tout réglé
    @Test
    fun groupDetailScreen_ongletSoldes_toutRegle() {
        val group = GroupData(
            id = 3L,
            groupName = "Dîner d'équipe",
            participants = listOf("Alice", "Bob"),
            expenses = listOf(
                Expense(1, "Restaurant", 60.0, "Alice"),
                Expense(2, "Dessert", 60.0, "Bob")
            )
        )
        val fmt = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
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
