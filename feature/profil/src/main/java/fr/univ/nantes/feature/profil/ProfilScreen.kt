package fr.univ.nantes.feature.profil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.Green500
import fr.univ.nantes.core.ui.Green700
import fr.univ.nantes.core.ui.Green900
import fr.univ.nantes.core.ui.GreenBg50
import fr.univ.nantes.core.ui.Teal600
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfilViewModel = koinViewModel(),
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveSuccessMessage = stringResource(id = R.string.profile_saved_success)

    LaunchedEffect(state.saveSuccess, saveSuccessMessage) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(message = saveSuccessMessage)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(state.shouldRedirectLogin) {
        if (state.shouldRedirectLogin && !state.isLoading) {
            viewModel.redirectToLogin(onLogout)
        }
    }

    ProfileScreenContent(
        state = state,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onLogout = { viewModel.logout(onLogout) },
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onEmailChange = viewModel::onEmailChange,
        onCurrencyChange = viewModel::onCurrencyChange,
        onSave = viewModel::saveProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onFirstNameChange: (String) -> Unit = {},
    onLastNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onCurrencyChange: (String) -> Unit = {},
    onSave: () -> Unit = {}
) {
    val currencies = state.currencies

    val selectedCurrencyLabel = currencies.firstOrNull { it.first == state.currency }
        ?.let { "${it.first} — ${it.second}" }
        ?: state.currency

    var currencyMenuExpanded by remember { mutableStateOf(false) }

    if (state.shouldRedirectLogin && !state.isLoading) {
        return
    }

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.profile_title),
                showBack = true,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Green500, Teal600))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            ProfileSectionCard(title = stringResource(id = R.string.profile_personal_information)) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = onFirstNameChange,
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.profile_first_name)) },
                    isError = state.errors.containsKey("firstName"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errors.containsKey("firstName")) {
                    ErrorText(state.errors.getValue("firstName"))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = onLastNameChange,
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.profile_last_name)) },
                    isError = state.errors.containsKey("lastName"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errors.containsKey("lastName")) {
                    ErrorText(state.errors.getValue("lastName"))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.profile_email)) },
                    isError = state.errors.containsKey("email"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errors.containsKey("email")) {
                    ErrorText(state.errors.getValue("email"))
                }
            }

            ProfileSectionCard(title = stringResource(id = R.string.profile_preferences)) {
                var currencySearch by remember { mutableStateOf("") }
                val filteredCurrencies = remember(currencies, currencySearch) {
                    if (currencySearch.isBlank()) {
                        currencies
                    } else {
                        currencies.filter { (code, name) ->
                            code.contains(currencySearch, ignoreCase = true) ||
                                name.contains(currencySearch, ignoreCase = true)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = currencyMenuExpanded,
                    onExpandedChange = { expanded ->
                        currencyMenuExpanded = expanded
                        if (!expanded) currencySearch = ""
                    }
                ) {
                    OutlinedTextField(
                        value = selectedCurrencyLabel,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.profile_preferred_currency)) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = {
                            currencyMenuExpanded = false
                            currencySearch = ""
                        }
                    ) {
                        OutlinedTextField(
                            value = currencySearch,
                            onValueChange = { currencySearch = it },
                            placeholder = { Text(stringResource(id = R.string.profile_currency_search_placeholder)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        HorizontalDivider()
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        // Scrollable Column instead of LazyColumn — LazyColumn (SubcomposeLayout)
                        // cannot be used inside ExposedDropdownMenu because the parent ScrollNode
                        // requests intrinsic measurements, which SubcomposeLayout does not support.
                        Column(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (filteredCurrencies.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.profile_currency_no_result),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                filteredCurrencies.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text("$code — $name") },
                                        onClick = {
                                            onCurrencyChange(code)
                                            currencyMenuExpanded = false
                                            currencySearch = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.profile_currency_conversion_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InfoBanner(preferredCurrency = state.currency)

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green500,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.profile_save_changes), fontWeight = FontWeight.SemiBold)
            }

            TextButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(id = R.string.profile_logout), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun InfoBanner(preferredCurrency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GreenBg50),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Public, contentDescription = null, tint = Green700)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(id = R.string.profile_automatic_conversion), fontWeight = FontWeight.SemiBold, color = Green900)
                Text(
                    stringResource(id = R.string.profile_preferred_currency_info, preferredCurrency),
                    style = MaterialTheme.typography.bodySmall,
                    color = Green900
                )
                Text(
                    stringResource(id = R.string.profile_conversion_calculation_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = Green900
                )
            }
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    AppliCashTheme {
        ProfileScreenContent(
            state = ProfileUiState(
                firstName = "Alice",
                lastName = "Martin",
                email = "alice@example.com",
                currency = "EUR",
                currencies = listOf("EUR" to "Euro", "USD" to "Dollar"),
                isLoading = false
            ),
            onBack = {},
            onLogout = {}
        )
    }
}
