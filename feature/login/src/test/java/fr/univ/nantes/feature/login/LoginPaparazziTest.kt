package fr.univ.nantes.feature.login

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
 * Tests snapshot pour LoginScreen.
 * RG1 : thème clair. CA1 : couverture écran login. CA2 : états initial, inscription, erreur, chargement.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class LoginPaparazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    // CA2 : état initial (connexion vide)
    @Test
    fun loginScreen_etatInitial() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(state = LoginUiState(), modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA2 : formulaire d'inscription complet
    @Test
    fun loginScreen_modeInscription() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(
                    state = LoginUiState(
                        isRegister = true,
                        email = "alice@example.com",
                        password = "••••••••",
                        firstName = "Alice",
                        lastName = "Martin",
                        currency = "EUR",
                        currencies = listOf("EUR" to "Euro", "USD" to "Dollar américain")
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA2 : état erreur
    @Test
    fun loginScreen_avecErreur() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(
                    state = LoginUiState(email = "invalide", errorMessage = "Email invalide"),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    // CA2 : état chargement
    @Test
    fun loginScreen_enChargement() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(
                    state = LoginUiState(
                        email = "alice@example.com",
                        password = "••••••••",
                        isLoading = true
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
