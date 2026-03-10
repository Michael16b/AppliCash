package fr.univ.nantes.feature.splashscreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests snapshot pour SplashScreen.
 * RG1 : thème clair. CA1 : couverture écran de démarrage.
 * splashDurationMs = Long.MAX_VALUE pour figer l'affichage (pas de navigation automatique).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SplashPaparazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    // CA1 : splash screen affiché (état statique, pas de navigation)
    @Test
    fun splashScreen_affiche() {
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
