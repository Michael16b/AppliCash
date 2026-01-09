package fr.univ.nantes.home

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fr.univ.nantes.core.ui.AppliCashTheme
import kotlinx.serialization.Serializable

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    name: String = "defaultUser",
) {
    Box(modifier) {
        Greeting(name = name, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Serializable
data class Home(val username: String = "Guest")

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppliCashTheme {
        HomeScreen(name = "Android")
    }
}
