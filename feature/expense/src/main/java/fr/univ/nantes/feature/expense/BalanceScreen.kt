package fr.univ.nantes.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.Green700
import fr.univ.nantes.core.ui.GreenBg50
import fr.univ.nantes.core.ui.TealBg50
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object BalanceRoute

@Composable
fun MemberBalanceCard(balance: Balance, formatter: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = balance.participant, fontWeight = FontWeight.Medium)

            val isPositive = balance.amount > 0.01
            val isNegative = balance.amount < -0.01

            Text(
                text = (if (isPositive) "+" else "") + formatter.format(balance.amount),
                color = when {
                    isPositive -> Green700
                    isNegative -> Color.Red
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettlementCard(reimbursement: Reimbursement, formatter: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(reimbursement.from, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.reimbursement_arrow),
                    tint = Green700
                )
                Text(reimbursement.to, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }

            Surface(
                modifier = Modifier.padding(top = 8.dp),
                color = TealBg50,
                shape = CircleShape
            ) {
                Text(
                    text = formatter.format(reimbursement.amount),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Green700,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun AllSettledView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenBg50, RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.all_settled),
                color = Green700,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BalanceScreen(
    viewModel: ExpenseViewModel
) {
    val state by viewModel.state.collectAsState()
    val balances by viewModel.balances.collectAsState()
    val reimbursements by viewModel.reimbursements.collectAsState()

    BalanceContent(
        groupName = state.groupName,
        balances = balances,
        reimbursements = reimbursements,
        userCurrencyCode = state.userCurrencyCode
    )
}

@Composable
fun BalanceContent(
    groupName: String,
    balances: List<Balance>,
    reimbursements: List<Reimbursement>,
    userCurrencyCode: String = "EUR"
) {
    val currencyFormatter = remember(userCurrencyCode) {
        val currency = java.util.Currency.getInstance(userCurrencyCode)
        val locale = when (userCurrencyCode) {
            "EUR" -> Locale.FRANCE
            "USD" -> Locale.US
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "CNY" -> Locale.CHINA
            "CAD" -> Locale.CANADA
            else -> Locale.getAvailableLocales().find { locale ->
                locale.country.isNotEmpty() &&
                    try {
                        java.util.Currency.getInstance(locale).currencyCode == userCurrencyCode
                    } catch (_: Exception) {
                        false
                    }
            } ?: Locale.getDefault()
        }
        NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = groupName, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.balance_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.member_balances),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(balances) { balance ->
                MemberBalanceCard(balance, currencyFormatter)
            }

            item {
                Text(
                    text = stringResource(R.string.balances_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.suggested_reimbursements),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (reimbursements.isEmpty()) {
                item { AllSettledView() }
            } else {
                items(reimbursements) { reimbursement ->
                    SettlementCard(reimbursement, currencyFormatter)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Dettes en cours")
@Composable
fun PreviewBalanceWithDebts() {
    MaterialTheme {
        BalanceContent(
            groupName = "Vacances à Nantes",
            balances = listOf(
                Balance("Alice", 25.50),
                Balance("Bob", -10.00),
                Balance("Charlie", -15.50)
            ),
            reimbursements = listOf(
                Reimbursement("Bob", "Alice", 10.00),
                Reimbursement("Charlie", "Alice", 15.50)
            )
        )
    }
}

@Preview(showBackground = true, name = "Tout est réglé")
@Composable
fun PreviewBalanceSettled() {
    MaterialTheme {
        BalanceContent(
            groupName = "Dîner Pizza",
            balances = listOf(
                Balance("Alice", 0.0),
                Balance("Bob", 0.0)
            ),
            reimbursements = emptyList()
        )
    }
}

@Preview(widthDp = 300)
@Composable
fun PreviewMemberCard() {
    val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    MaterialTheme {
        Column(Modifier.padding(10.dp)) {
            MemberBalanceCard(Balance("Alice", 45.0), formatter)
            Spacer(Modifier.height(8.dp))
            MemberBalanceCard(Balance("Bob", -20.0), formatter)
        }
    }
}
