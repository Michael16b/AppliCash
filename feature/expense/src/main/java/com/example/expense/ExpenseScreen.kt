package com.example.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
data object ExpenseRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    navigateToBalance: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                var dotSeen = false
                val filtered = buildString {
                    for (c in input) {
                        when {
                            c.isDigit() -> append(c)
                            c == '.' && !dotSeen -> {
                                append(c)
                                dotSeen = true
                            }
                            // ignore other characters and additional dots
                        }
                    }
                }
                amount = filtered
            },
            label = { Text("Montant") },
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
                label = { Text("Payé par") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
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

        Button(
            onClick = {
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                viewModel.addExpense(description, amountValue, selectedPayer)
                description = ""
                amount = ""
                selectedPayer = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = description.isNotBlank() && amount.isNotBlank() && selectedPayer.isNotBlank()
        ) {
            Text("Ajouter dépense")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Dépenses (${state.expenses.size})",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(state.expenses) { expense ->
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
                                text = "Payé par ${expense.paidBy}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = String.format("%.2f EUR", expense.amount),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Button(
            onClick = navigateToBalance,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.expenses.isNotEmpty()
        ) {
            Text("Voir le partage")
        }
    }
}
