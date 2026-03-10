package fr.univ.nantes.feature.profil

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

/**
 * Snapshot tests for ProfileScreen.
 *
 * RG1: light theme for each state.
 * CA1: covers the profile screen.
 * CA2: loading state, success (profile loaded), validation errors.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ProfilPaparazziTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** CA2: loading state */
    @Test
    fun profileScreen_loading() {
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

    /** CA1 + CA2: success state -- profile loaded */
    @Test
    fun profileScreen_profileLoaded() {
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
                            "USD" to "US Dollar",
                            "GBP" to "British Pound"
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

    /** CA2: error state -- validation errors */
    @Test
    fun profileScreen_validationErrors() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                ProfileScreenContent(
                    state = ProfileUiState(
                        firstName = "",
                        lastName = "",
                        email = "invalid",
                        currency = "EUR",
                        currencies = listOf("EUR" to "Euro"),
                        isLoading = false,
                        errors = mapOf(
                            "firstName" to "First name is required",
                            "lastName" to "Last name is required",
                            "email" to "Invalid email"
                        )
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
