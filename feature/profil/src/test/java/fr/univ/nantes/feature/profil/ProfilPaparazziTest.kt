package fr.univ.nantes.feature.profil

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

/**
 * Tests snapshot pour ProfileScreen.
 * RG1 : thème clair. CA1 : couverture profil. CA2 : chargement, succès, erreurs validation.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ProfilPaparazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    // CA2 : état chargement
    @Test
    fun profileScreen_enChargement() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                ProfileScreenContent(
                    state = ProfileUiState(isLoading = true),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA1 + CA2 : état succès — profil chargé
    @Test
    fun profileScreen_profilCharge() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                ProfileScreenContent(
                    state = ProfileUiState(
                        firstName = "Alice",
                        lastName = "Martin",
                        email = "alice@example.com",
                        currency = "EUR",
                        currencies = listOf(
                            "EUR" to "Euro",
                            "USD" to "Dollar américain",
                            "GBP" to "Livre sterling"
                        ),
                        isExistingProfile = true,
                        isLoading = false
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA2 : état erreur — erreurs de validation
    @Test
    fun profileScreen_avecErreursValidation() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                ProfileScreenContent(
                    state = ProfileUiState(
                        firstName = "",
                        lastName = "",
                        email = "invalide",
                        currency = "EUR",
                        currencies = listOf("EUR" to "Euro"),
                        isLoading = false,
                        errors = mapOf(
                            "firstName" to "Le prénom est requis",
                            "lastName" to "Le nom est requis",
                            "email" to "Email invalide"
                        )
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
