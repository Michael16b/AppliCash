package fr.univ.nantes.feature.profil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.AppTopBar
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
    val currencies = listOf("EUR - Euro", "USD - Dollar", "GBP - Livre", "JPY - Yen")
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Mon profil",
                showBack = true,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF10B981), Color(0xFF0D9488))
                            )
                        ),
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

            ProfileSectionCard(title = "Informations personnelles") {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Prénom") },
                    isError = state.errors.containsKey("firstName"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errors.containsKey("firstName")) {
                    ErrorText(state.errors.getValue("firstName"))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Nom") },
                    isError = state.errors.containsKey("lastName"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errors.containsKey("lastName")) {
                    ErrorText(state.errors.getValue("lastName"))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    label = { Text("Adresse email") },
                    isError = state.errors.containsKey("email"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errors.containsKey("email")) {
                    ErrorText(state.errors.getValue("email"))
                }
            }

            ProfileSectionCard(title = "Préférences") {
                ExposedDropdownMenuBox(
                    expanded = currencyMenuExpanded,
                    onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = state.currency,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
                        label = { Text("Devise préférée") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    viewModel.onCurrencyChange(currency)
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Les montants seront automatiquement convertis dans votre devise préférée",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InfoBanner(preferredCurrency = state.currency)

            Button(
                onClick = { viewModel.saveProfile { } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Icon(Icons.Outlined.Public, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enregistrer les modifications")
            }

            TextButton(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Public, contentDescription = null, tint = Color(0xFF16A34A))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Conversion automatique", fontWeight = FontWeight.SemiBold, color = Color(0xFF166534))
                Text(
                    "Votre devise préférée : $preferredCurrency",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF166534)
                )
                Text(
                    "Les dépenses dans d\'autres devises seront automatiquement converties pour les calculs de soldes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF166534)
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
        ProfileScreen(onBack = {}, onLogout = {})
    }
}
