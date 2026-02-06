package fr.univ.nantes.feature.splashscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import fr.univ.nantes.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// Objet de navigation pour SplashScreen
@Serializable
object Splash

@Composable
fun SplashScreen(navigateToGroup: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(4000)
        navigateToGroup()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "AppliCash Logo"
        )
    }
}
