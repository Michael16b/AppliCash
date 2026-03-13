package fr.univ.nantes.feature.expense

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object ExpenseRoute

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    navigateToBalance: () -> Unit,
    onStartCamera: () -> Unit = {},
    receiptPreviewPath: String? = null,
    onClearReceipt: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val convertedExpenses by viewModel.convertedCurrentExpenses.collectAsState()
    val loginRequiredMessage = stringResource(id = R.string.login_required_add_expense)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isLoggedIn, loginRequiredMessage) {
        if (!state.isLoggedIn) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = loginRequiredMessage,
                    withDismissAction = true
                )
            }
        }
    }

    val currencyCode = state.userCurrencyCode
    val currencyFormatter = remember(currencyCode) {
        val currency = java.util.Currency.getInstance(currencyCode)
        // Map common currency codes to locales for efficiency
        val matchingLocale = when (currencyCode) {
            "EUR" -> Locale.FRANCE
            "USD" -> Locale.US
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "CNY" -> Locale.CHINA
            "CAD" -> Locale.CANADA
            else -> {
                // Fallback: find a locale that uses this currency
                Locale.getAvailableLocales().find { locale ->
                    locale.country.isNotEmpty() &&
                        try {
                            java.util.Currency.getInstance(locale).currencyCode == currencyCode
                        } catch (_: IllegalArgumentException) {
                            false
                        }
                } ?: Locale.getDefault()
            }
        }
        NumberFormat.getCurrencyInstance(matchingLocale).apply {
            this.currency = currency
        }
    }

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier
                            .padding(12.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(data.visuals.message)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = state.groupName,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    var dotSeen = false
                    var hasDigitBeforeDot = false
                    val filtered = buildString {
                        for (c in input) {
                            when {
                                c.isDigit() -> {
                                    append(c)
                                    if (!dotSeen) hasDigitBeforeDot = true
                                }
                                c == '.' && !dotSeen && hasDigitBeforeDot -> {
                                    // Only allow dot if there's at least one digit before it
                                    append(c)
                                    dotSeen = true
                                }
                                // ignore other characters and additional dots
                            }
                        }
                    }
                    // Only accept if empty (clearing) or has at least one digit
                    if (filtered.isEmpty() || filtered.any { it.isDigit() }) {
                        amount = filtered
                    }
                },
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedPayer,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.paid_by)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    state.participants.forEach { participant ->
                        DropdownMenuItem(
                            text = { Text(participant) },
                            onClick = {
                                selectedPayer = participant
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera button and lightweight preview
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onStartCamera() }) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = stringResource(R.string.take_photo))
                }
                receiptPreviewPath?.let { path ->
                    Row {
                        Text(text = path)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { onStartCamera() }) {
                            Text(stringResource(R.string.retake_photo))
                        }
                        Button(onClick = onClearReceipt) {
                            Text(stringResource(R.string.remove_photo))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (!state.isLoggedIn) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = loginRequiredMessage,
                                withDismissAction = true
                            )
                        }
                        return@Button
                    }
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    viewModel.addExpense(description, amountValue, selectedPayer, receiptPath = receiptPreviewPath)
                    description = ""
                    amount = ""
                    selectedPayer = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() && amount.isNotBlank() && selectedPayer.isNotBlank()
            ) {
                Text(stringResource(R.string.add_expense))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.expenses_count, convertedExpenses.size),
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(convertedExpenses) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(expense.description)
                                Text(
                                    text = stringResource(R.string.paid_by_format, expense.paidBy),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = currencyFormatter.format(expense.amount),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Button(
                onClick = navigateToBalance,
                modifier = Modifier.fillMaxWidth(),
                enabled = convertedExpenses.isNotEmpty()
            ) {
                Text(stringResource(R.string.view_split))
            }
        }
    }
}
