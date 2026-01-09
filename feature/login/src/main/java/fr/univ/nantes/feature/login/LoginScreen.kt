package fr.univ.nantes.feature.login

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.Purple40
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val username by viewModel.username.collectAsState(viewModel.defaultUsername)
    val password by viewModel.password.collectAsState("")
    val errorMessage by viewModel.errorMessage.collectAsState(null)

    LoginScreenStateless(
        modifier = modifier,
        username = username,
        setUsername = viewModel.setUsername,
        password = password,
        setPassword = viewModel.setPassword,
        onLogin = { viewModel.onLoginClick { navigateToHome() } },
        errorMessage = errorMessage,
        clearError = viewModel.clearError,
    )
}

@Composable
private fun LoginScreenStateless(
    modifier: Modifier,
    username: String,
    setUsername: (String) -> Unit,
    password: String,
    setPassword: (String) -> Unit,
    onLogin: () -> Unit,
    errorMessage: String?,
    clearError: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { 
                setUsername(it)
                clearError()
            },
            label = { Text("Username") },
            colors =
                OutlinedTextFieldDefaults.colors().copy(
                    focusedTextColor = Purple40,
                    unfocusedTextColor = Purple40,
                ),
            isError = errorMessage != null,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { 
                setPassword(it)
                clearError()
            },
            label = { Text("Password") },
            colors =
                OutlinedTextFieldDefaults.colors().copy(
                    focusedTextColor = Purple40,
                    unfocusedTextColor = Purple40,
                ),
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage != null,
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            )
        }

        Button(onClick = {
            Log.d(
                "LoginScreen",
                "Login button clicked with username: $username and password: $password",
            )
            onLogin()
        }) {
            Text("Login")
        }
    }
}

@Serializable
data object Login

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AppliCashTheme {
        LoginScreenStateless(
            username = "toto",
            setUsername = {},
            password = "toto",
            setPassword = {},
            onLogin = { },
            modifier = Modifier.fillMaxSize(),
            errorMessage = null,
            clearError = {},
        )
    }
}
