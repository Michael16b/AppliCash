package fr.univ.nantes.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppliCashTheme
import kotlinx.serialization.Serializable

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    name: String = "defaultUser",
    navigateToExpense: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Greeting(name = name)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = navigateToExpense,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Expense Tracking")
        }
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
        HomeScreen(name = "Android", navigateToExpense = {})
    }
}
