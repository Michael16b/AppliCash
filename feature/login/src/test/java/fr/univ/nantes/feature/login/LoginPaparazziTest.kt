package fr.univ.nantes.feature.login

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
 * Snapshot tests for LoginScreen.
 *
 * RG1: light theme.
 * CA1: covers the login screen.
 * CA2: initial state, register mode, error state, loading state.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class LoginPaparazziTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** CA2: initial state (empty login form) */
    @Test
    fun loginScreen_initialState() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(state = LoginUiState(), modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: register mode with filled form */
    @Test
    fun loginScreen_registerMode() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(
                    state = LoginUiState(
                        isRegister = true,
                        email = "alice@example.com",
                        password = "password",
                        firstName = "Alice",
                        lastName = "Martin",
                        currency = "EUR",
                        currencies = listOf("EUR" to "Euro", "USD" to "US Dollar")
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: error state */
    @Test
    fun loginScreen_withError() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(
                    state = LoginUiState(email = "invalid", errorMessage = "Invalid email"),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** CA2: loading state */
    @Test
    fun loginScreen_loading() {
        composeRule.setContent {
            AppliCashTheme(dynamicColor = false) {
                LoginScreenContent(
                    state = LoginUiState(
                        email = "alice@example.com",
                        password = "password",
                        isLoading = true
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
