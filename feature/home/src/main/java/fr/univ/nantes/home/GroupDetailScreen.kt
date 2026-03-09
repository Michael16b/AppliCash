package fr.univ.nantes.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.Green700
import fr.univ.nantes.core.ui.GreenBg50
import fr.univ.nantes.core.ui.Teal400
import fr.univ.nantes.core.ui.TealBg50
import fr.univ.nantes.feature.expense.Balance
import fr.univ.nantes.feature.expense.Expense
import fr.univ.nantes.feature.expense.GroupData
import fr.univ.nantes.feature.expense.Reimbursement
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class GroupDetail(val groupId: Long)

@Composable
fun GroupDetailScreen(
    group: GroupData,
    onBack: () -> Unit,
    onAddExpense: () -> Unit = {},
    onDeleteExpense: (Long) -> Unit = {},
    isLoggedIn: Boolean = true,
    onRequireLogin: () -> Unit = {}
) {
    val currencyCode = "EUR"
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(currencyCode)
        }
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val loginMessage = stringResource(id = fr.univ.nantes.feature.expense.R.string.login_required_add_expense)
    val loginAction = stringResource(id = fr.univ.nantes.feature.expense.R.string.login_action)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = group.groupName.ifBlank { stringResource(R.string.home_title) },
                subtitle = "${group.participants.size} ${stringResource(if (group.participants.size != 1) R.string.members else R.string.member)}",
                showBack = true,
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (isLoggedIn) {
                            onAddExpense()
                        } else {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = loginMessage,
                                    actionLabel = loginAction,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onRequireLogin()
                                }
                            }
                        }
                    },
                    containerColor = Teal400
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_expense),
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HeaderSummary(group, currencyFormat)
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = {
                    SecondaryIndicator(
                        color = Teal400,
                        height = 3.dp,
                        modifier = Modifier.tabIndicatorOffset(selectedTab, matchContentSize = false)
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_expenses),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = Teal400,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_balances),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = Teal400,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (selectedTab) {
                0 -> ExpensesTab(group, currencyFormat, onDeleteExpense)
                1 -> BalancesTab(group, currencyFormat)
            }
        }
    }
}

@Composable
private fun HeaderSummary(group: GroupData, currencyFormat: NumberFormat) {
    val currencyCode = "EUR"
    val total = group.expenses.sumOf { it.amount }
    val isDarkMode = isSystemInDarkTheme()
    val cardColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceContainerHigh else TealBg50
    val cardTextColor = MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = cardTextColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.total_expenses),
                style = MaterialTheme.typography.labelMedium,
                color = cardTextColor.copy(alpha = 0.8f)
            )
            Text(
                text = currencyFormat.format(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cardTextColor
            )
            Text(
                text = stringResource(R.string.currency_label, currencyCode),
                style = MaterialTheme.typography.labelSmall,
                color = cardTextColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ExpensesTab(
    group: GroupData,
    currencyFormat: NumberFormat,
    onDeleteExpense: (Long) -> Unit
) {
    if (group.expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(group.expenses, key = { it.id }) { expense ->
            ExpenseItem(expense, group, currencyFormat, onDeleteExpense)
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: Expense,
    group: GroupData,
    currencyFormat: NumberFormat,
    onDeleteExpense: (Long) -> Unit
) {
    val sharePerPerson = if (group.participants.isNotEmpty()) {
        expense.amount / group.participants.size
    } else {
        0.0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currencyFormat.format(expense.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onDeleteExpense(expense.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_expense),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.paid_by, expense.paidBy),
                style = MaterialTheme.typography.bodySmall,
                color = Teal400,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = stringResource(R.string.participants_list_short, group.participants.joinToString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Card(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.per_person_amount, currencyFormat.format(sharePerPerson)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BalancesTab(group: GroupData, currencyFormat: NumberFormat) {
    val balances = remember(group) { calculateBalances(group) }
    val reimbursements = remember(balances) { calculateReimbursements(balances) }

    if (group.participants.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_members),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Section : Soldes des membres ──────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.balances_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (balances.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_expenses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(balances) { balance ->
                BalanceMemberRow(balance, currencyFormat)
            }

            item {
                Text(
                    text = stringResource(R.string.balances_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }
        }

        // ── Séparateur ────────────────────────────────────────────────────────
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        // ── Section : Remboursements suggérés ─────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.suggested_reimbursements),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (reimbursements.isEmpty()) {
            item { AllSettledBanner() }
        } else {
            items(reimbursements) { item ->
                ReimbursementRow(item, currencyFormat)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// ── Carte solde d'un membre ───────────────────────────────────────────────────
@Composable
private fun BalanceMemberRow(balance: Balance, currencyFormat: NumberFormat) {
    val isPositive = balance.amount > BALANCE_THRESHOLD
    val isNegative = balance.amount < -BALANCE_THRESHOLD

    val (bgColor, amountColor, labelText, _) = when {
        isPositive -> BalanceStyle(
            bg = GreenBg50,
            amount = Green700,
            label = stringResource(R.string.balance_to_receive),
            iconVec = Icons.Default.Person
        )
        isNegative -> BalanceStyle(
            bg = Color(0xFFFFF1F1),
            amount = Color(0xFFD32F2F),
            label = stringResource(R.string.balance_to_pay),
            iconVec = Icons.Default.Person
        )
        else -> BalanceStyle(
            bg = TealBg50,
            amount = Teal400,
            label = stringResource(R.string.balance_settled),
            iconVec = Icons.Default.Check
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar initiale
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(amountColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = balance.participant.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nom + label
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = balance.participant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = amountColor.copy(alpha = 0.8f)
                )
            }

            // Montant avec signe
            val sign = when {
                isPositive -> "+"
                isNegative -> "-"
                else -> ""
            }
            val displayAmount = if (isNegative) -balance.amount else balance.amount
            Text(
                text = sign + currencyFormat.format(displayAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor
            )
        }
    }
}

private data class BalanceStyle(
    val bg: Color,
    val amount: Color,
    val label: String,
    val iconVec: androidx.compose.ui.graphics.vector.ImageVector
)

// ── Carte remboursement suggéré ───────────────────────────────────────────────
@Composable
private fun ReimbursementRow(reimbursement: Reimbursement, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Débiteur
            Text(
                text = reimbursement.from,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )

            // Flèche + montant au centre
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.reimbursement_arrow),
                    tint = Teal400,
                    modifier = Modifier.size(18.dp)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(TealBg50, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = currencyFormat.format(reimbursement.amount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Teal400
                    )
                }
            }

            // Créditeur
            Text(
                text = reimbursement.to,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Green700,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Bannière "Tout est réglé" ─────────────────────────────────────────────────
@Composable
private fun AllSettledBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreenBg50),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Green700.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Green700,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.all_settled),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Green700
            )
        }
    }
}

private const val BALANCE_THRESHOLD = 0.01

private fun calculateBalances(group: GroupData): List<Balance> {
    if (group.participants.isEmpty()) return emptyList()
    val total = group.expenses.sumOf { it.amount }
    val share = total / group.participants.size
    val paidBy = group.participants.associateWith { participant ->
        group.expenses.filter { it.paidBy == participant }.sumOf { it.amount }
    }
    return group.participants.map { participant ->
        Balance(participant, (paidBy[participant] ?: 0.0) - share)
    }
}

private fun calculateReimbursements(balances: List<Balance>): List<Reimbursement> {
    val reimbursements = mutableListOf<Reimbursement>()
    val debtors = balances.filter { it.amount < -BALANCE_THRESHOLD }.sortedBy { it.amount }.toMutableList()
    val creditors = balances.filter { it.amount > BALANCE_THRESHOLD }.sortedByDescending { it.amount }.toMutableList()
    var i = 0
    var j = 0
    while (i < debtors.size && j < creditors.size) {
        val debtor = debtors[i]
        val creditor = creditors[j]
        val amount = minOf(-debtor.amount, creditor.amount)
        reimbursements.add(Reimbursement(debtor.participant, creditor.participant, amount))
        debtors[i] = debtor.copy(amount = debtor.amount + amount)
        creditors[j] = creditor.copy(amount = creditor.amount - amount)
        if (debtors[i].amount >= -BALANCE_THRESHOLD) i++
        if (creditors[j].amount <= BALANCE_THRESHOLD) j++
    }
    return reimbursements
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun GroupDetailPreview() {
    val group = GroupData(
        id = 1,
        groupName = "Vacances été 2025",
        participants = listOf("Alice", "Bob", "Charlie"),
        expenses = listOf(
            Expense(1, "Essence", 80.0, "Alice"),
            Expense(2, "Hôtel", 300.0, "Bob"),
            Expense(3, "Restaurant", 120.0, "Alice")
        )
    )
    AppliCashTheme {
        GroupDetailScreen(group = group, onBack = {})
    }
}

@Preview(showBackground = true, name = "Onglet Soldes - Dettes en cours")
@Composable
private fun BalancesTabPreview() {
    val group = GroupData(
        id = 2,
        groupName = "Week-end ski",
        participants = listOf("Julie", "Marc", "Sophie"),
        expenses = listOf(
            Expense(1, "Location chalet", 300.0, "Marc"),
            Expense(2, "Forfait ski", 150.0, "Julie"),
            Expense(3, "Courses", 60.0, "Sophie")
        )
    )
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
        currency = Currency.getInstance("EUR")
    }
    AppliCashTheme {
        BalancesTab(group = group, currencyFormat = currencyFormat)
    }
}

@Preview(showBackground = true, name = "Onglet Soldes - Tout réglé")
@Composable
private fun BalancesTabSettledPreview() {
    val group = GroupData(
        id = 3,
        groupName = "Dîner d'équipe",
        participants = listOf("Alice", "Bob"),
        expenses = listOf(
            Expense(1, "Restaurant", 60.0, "Alice"),
            Expense(2, "Dessert", 60.0, "Bob")
        )
    )
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
        currency = Currency.getInstance("EUR")
    }
    AppliCashTheme {
        BalancesTab(group = group, currencyFormat = currencyFormat)
    }
}
