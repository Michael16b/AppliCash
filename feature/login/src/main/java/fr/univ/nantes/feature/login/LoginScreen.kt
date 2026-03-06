package fr.univ.nantes.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.Green500
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navigateToHome: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = if (state.isRegister) stringResource(R.string.login_create_account) else stringResource(R.string.login_login)) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::setEmail,
                label = { Text(stringResource(R.string.login_email)) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage != null
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text(stringResource(R.string.login_password)) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage != null
            )
            if (state.isRegister) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::setFirstName,
                    label = { Text(stringResource(R.string.login_first_name)) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.errorMessage != null
                )
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::setLastName,
                    label = { Text(stringResource(R.string.login_last_name)) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.errorMessage != null
                )
                ExposedDropdownMenuBox(
                    expanded = currencyMenuExpanded,
                    onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.login_preferred_currency)) },
                        leadingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        val currencies = state.currencies.ifEmpty { listOf("EUR - Euro") }
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    viewModel.setCurrency(currency)
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.submit(navigateToHome) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green500,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
            ) {
                Text(if (state.isRegister) stringResource(R.string.login_create_account) else stringResource(R.string.login_login), fontWeight = FontWeight.SemiBold)
            }

            TextButton(onClick = viewModel::toggleMode) {
                Text(if (state.isRegister) stringResource(R.string.login_already_have_account) else stringResource(R.string.login_create_account_prompt))
            }
        }
    }
}

@Serializable
object Login

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AppliCashTheme {
        LoginScreen(navigateToHome = {}, modifier = Modifier.fillMaxSize())
    }
}
