package fr.univ.nantes.feature.expense

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
data object BalanceRoute

@Composable
fun BalanceScreen(
    viewModel: ExpenseViewModel,
    navigateToGroup: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val reimbursements by viewModel.reimbursements.collectAsState()
    val total = state.expenses.sumOf { it.amount }
    
    val currencyCode = stringResource(R.string.currency_code)
    val currencyFormatter = remember(currencyCode) {
        val currency = java.util.Currency.getInstance(currencyCode)
        val locale = java.util.Currency.getAvailableLocales()
            .firstOrNull { java.util.Currency.getInstance(it).currencyCode == currencyCode }
            ?: Locale.getDefault()
        NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
        }
    }

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
            text = stringResource(R.string.total_format, currencyFormatter.format(total)),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text(
                    text = stringResource(R.string.balances),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
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
                                "+" + currencyFormatter.format(balance.amount)
                            } else {
                                currencyFormatter.format(balance.amount)
                            },
                            color = if (balance.amount >= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.reimbursements),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (reimbursements.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_reimbursement),
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
                            text = stringResource(
                                R.string.owes_format,
                                reimbursement.from,
                                currencyFormatter.format(reimbursement.amount),
                                reimbursement.to
                            ),
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
            Text(stringResource(R.string.new_group_button))
        }
    }
}
