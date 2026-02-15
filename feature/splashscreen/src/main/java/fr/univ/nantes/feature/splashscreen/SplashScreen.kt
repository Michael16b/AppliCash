package fr.univ.nantes.feature.splashscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// Objet de navigation pour SplashScreen
@Serializable
object Splash

@Composable
fun SplashScreen(
    navigateNext: () -> Unit,
    splashDurationMs: Long = 2000L
) {
    LaunchedEffect(Unit) {
        delay(splashDurationMs)
        navigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
    }
}
