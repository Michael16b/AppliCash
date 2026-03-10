package fr.univ.nantes.feature.splashscreen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot tests for SplashScreen.
 *
 * RG1: light theme.
 * CA1: covers the splash screen.
 * splashDurationMs is set to Long.MAX_VALUE to freeze the display (no auto-navigation).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SplashPaparazziTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** CA1: splash screen displayed (static state, no navigation) */
    @Test
    fun splashScreen_displayed() {
        composeRule.setContent {
            MaterialTheme {
                SplashScreen(
                    navigateNext = {},
                    splashDurationMs = Long.MAX_VALUE
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
