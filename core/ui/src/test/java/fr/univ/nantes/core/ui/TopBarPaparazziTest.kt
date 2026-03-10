package fr.univ.nantes.core.ui

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
 * Tests snapshot pour le composant AppTopBar (RG2 — composants réutilisables testés en isolation).
 * RG1 : thème clair.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TopBarPaparazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topBar_titreSeul() {
        composeRule.setContent {
            MaterialTheme {
                AppTopBar(title = "AppliCash")
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun topBar_avecSousTitre() {
        composeRule.setContent {
            MaterialTheme {
                AppTopBar(
                    title = "Accueil",
                    subtitle = "Gérez vos dépenses"
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun topBar_avecBoutonRetour() {
        composeRule.setContent {
            MaterialTheme {
                AppTopBar(
                    title = "Détail du groupe",
                    subtitle = "3 membres",
                    showBack = true,
                    onBack = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
