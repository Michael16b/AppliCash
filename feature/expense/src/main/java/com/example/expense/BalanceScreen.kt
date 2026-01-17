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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
data object BalanceRoute

@Composable
fun BalanceScreen(
    viewModel: ExpenseViewModel,
    navigateToGroup: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val balances = viewModel.calculateBalances()
    val reimbursements = viewModel.calculateReimbursements()
    val total = state.expenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = state.groupName,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Total: ${"%.2f".format(total)} EUR",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Soldes",
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(balances) { balance ->
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
                        Text(balance.participant)
                        Text(
                            text = if (balance.amount >= 0) {
                                "+${"%.2f".format(balance.amount)} EUR"
                            } else {
                                "${"%.2f".format(balance.amount)} EUR"
                            },
                            color = if (balance.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Remboursements",
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            if (reimbursements.isEmpty()) {
                item {
                    Text(
                        text = "Aucun remboursement nécessaire",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(reimbursements) { reimbursement ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${reimbursement.from} doit ${"%.2f".format(reimbursement.amount)} EUR à ${reimbursement.to}",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.reset()
                navigateToGroup()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nouveau groupe")
        }
    }
}
