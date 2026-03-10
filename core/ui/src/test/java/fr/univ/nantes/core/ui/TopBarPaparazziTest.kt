package fr.univ.nantes.core.ui

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
 * Snapshot tests for the AppTopBar component.
 *
 * RG2: reusable components tested in isolation.
 * RG1: light theme.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TopBarPaparazziTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Title only */
    @Test
    fun topBar_titleOnly() {
        composeRule.setContent {
            MaterialTheme {
                AppTopBar(title = "AppliCash")
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** Title with subtitle */
    @Test
    fun topBar_withSubtitle() {
        composeRule.setContent {
            MaterialTheme {
                AppTopBar(
                    title = "Home",
                    subtitle = "Manage your expenses"
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** Title with back button */
    @Test
    fun topBar_withBackButton() {
        composeRule.setContent {
            MaterialTheme {
                AppTopBar(
                    title = "Group detail",
                    subtitle = "3 members",
                    showBack = true,
                    onBack = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
