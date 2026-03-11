package fr.univ.nantes.home

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.Green700
import fr.univ.nantes.core.ui.GreenBg50
import fr.univ.nantes.core.ui.Red700
import fr.univ.nantes.core.ui.RedBg50
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

/** Source currency for all expenses entered in the app. */
private const val EXPENSE_CURRENCY = "EUR"

@Serializable
data class GroupDetail(val groupId: Long)

@SuppressLint("RememberReturnType")
@Composable
fun GroupDetailScreen(
    group: GroupData,
    onBack: () -> Unit,
    onAddExpense: () -> Unit = {},
    onDeleteExpense: (Long) -> Unit = {},
    onEditGroup: () -> Unit = {},
    isLoggedIn: Boolean = true,
    onRequireLogin: () -> Unit = {},
    userCurrencyCode: String = EXPENSE_CURRENCY,
    convertAmount: suspend (Double, String) -> Double? = { amount, _ -> amount }
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val originalFormat = remember(EXPENSE_CURRENCY) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(EXPENSE_CURRENCY)
        }
    }
    val userFormat = remember(userCurrencyCode) {
        runCatching {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                currency = Currency.getInstance(userCurrencyCode)
            }
        }.getOrDefault(originalFormat)
    }
    val showConversion = userCurrencyCode.uppercase() != EXPENSE_CURRENCY

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
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEditGroup) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_group),
                            tint = Color.White
                        )
                    }
                }
            )
        },
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
            HeaderSummary(
                group = group,
                originalFormat = originalFormat,
                userFormat = userFormat,
                userCurrencyCode = userCurrencyCode,
                showConversion = showConversion,
                convertAmount = convertAmount
            )

            if (group.shareCode.isNotBlank() && group.shareCode.isNotBlank()) {
                val shareMessage = stringResource(R.string.share_message, group.groupName, group.shareCode)

                ShareCodeCard(
                    shareCode = group.shareCode,
                    onCopyCode = {
                        scope.launch {
                            clipboardManager.setText(AnnotatedString(group.shareCode))
                        }
                    },
                    onShareCode = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
            }
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
                0 -> ExpensesTab(
                    group = group,
                    originalFormat = originalFormat,
                    userFormat = userFormat,
                    showConversion = showConversion,
                    convertAmount = convertAmount,
                    onDeleteExpense = onDeleteExpense
                )
                1 -> BalancesTab(
                    group = group,
                    originalFormat = originalFormat,
                    userFormat = userFormat,
                    showConversion = showConversion,
                    convertAmount = convertAmount
                )
            }
        }
    }
}

@Composable
private fun HeaderSummary(
    group: GroupData,
    originalFormat: NumberFormat,
    userFormat: NumberFormat,
    userCurrencyCode: String,
    showConversion: Boolean,
    convertAmount: suspend (Double, String) -> Double?
) {
    val total = group.expenses.sumOf { it.amount }
    var convertedTotal by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(total, userCurrencyCode) {
        convertedTotal = if (showConversion) convertAmount(total, EXPENSE_CURRENCY) else total
    }

    val isDarkMode = isSystemInDarkTheme()
    val cardColor = if (isDarkMode) MaterialTheme.colorScheme.surfaceContainerHigh else TealBg50
    val cardTextColor = MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = cardTextColor),
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
            if (showConversion && convertedTotal != null) {
                Text(
                    text = userFormat.format(convertedTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.original_amount, originalFormat.format(total)),
                    style = MaterialTheme.typography.labelSmall,
                    color = cardTextColor.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = originalFormat.format(total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )
            }
            Text(
                text = stringResource(R.string.currency_label, if (showConversion) userCurrencyCode else EXPENSE_CURRENCY),
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
    originalFormat: NumberFormat,
    userFormat: NumberFormat,
    showConversion: Boolean,
    convertAmount: suspend (Double, String) -> Double?,
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
            ExpenseItem(
                expense = expense,
                group = group,
                originalFormat = originalFormat,
                userFormat = userFormat,
                showConversion = showConversion,
                convertAmount = convertAmount,
                onDeleteExpense = onDeleteExpense
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseItem(
    expense: Expense,
    group: GroupData,
    originalFormat: NumberFormat,
    userFormat: NumberFormat,
    showConversion: Boolean,
    convertAmount: suspend (Double, String) -> Double?,
    onDeleteExpense: (Long) -> Unit
) {
    val sharePerPerson = if (group.participants.isNotEmpty()) {
        expense.amount / group.participants.size
    } else {
        0.0
    }

    var convertedAmount by remember(expense.id) { mutableStateOf<Double?>(null) }
    var convertedShare by remember(expense.id) { mutableStateOf<Double?>(null) }

    LaunchedEffect(expense.amount, showConversion) {
        if (showConversion) {
            convertedAmount = convertAmount(expense.amount, EXPENSE_CURRENCY)
            convertedShare = convertAmount(sharePerPerson, EXPENSE_CURRENCY)
        }
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
                    Column(horizontalAlignment = Alignment.End) {
                        val displayAmount = if (showConversion && convertedAmount != null) {
                            userFormat.format(convertedAmount)
                        } else {
                            originalFormat.format(expense.amount)
                        }
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showConversion && convertedAmount != null) {
                            Text(
                                text = stringResource(R.string.original_amount, originalFormat.format(expense.amount)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Split info
            val splitLabel = when (expense.splitType) {
                2 -> stringResource(fr.univ.nantes.feature.expense.R.string.split_by_amount)
                1 -> stringResource(fr.univ.nantes.feature.expense.R.string.split_by_share)
                else -> stringResource(fr.univ.nantes.feature.expense.R.string.split_equally)
            }

            Text(
                text = splitLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Teal400,
                modifier = Modifier.padding(top = 6.dp)
            )

            if ((expense.splitType == 1 || expense.splitType == 2) && expense.splitDetails.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    expense.splitDetails.forEach { (participant, participantAmount) ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$participant · ${originalFormat.format(participantAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (expense.splitType == 0) {
                val sharePerPerson = if (group.participants.isNotEmpty()) {
                    expense.amount / group.participants.size
                } else {
                    0.0
                }
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${originalFormat.format(sharePerPerson)}/pers. · ${group.participants.joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Card(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (showConversion && convertedShare != null) {
                            "${userFormat.format(convertedShare)} ${stringResource(R.string.per_person_suffix)} (${originalFormat.format(sharePerPerson)})"
                        } else {
                            stringResource(R.string.per_person_amount, originalFormat.format(sharePerPerson))
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun BalancesTab(
    group: GroupData,
    originalFormat: NumberFormat,
    userFormat: NumberFormat,
    showConversion: Boolean,
    convertAmount: suspend (Double, String) -> Double?
) {
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
                BalanceMemberRow(
                    balance = balance,
                    originalFormat = originalFormat,
                    userFormat = userFormat,
                    showConversion = showConversion,
                    convertAmount = convertAmount
                )
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

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

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
                ReimbursementRow(
                    reimbursement = item,
                    originalFormat = originalFormat,
                    userFormat = userFormat,
                    showConversion = showConversion,
                    convertAmount = convertAmount
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun BalanceMemberRow(
    balance: Balance,
    originalFormat: NumberFormat,
    userFormat: NumberFormat,
    showConversion: Boolean,
    convertAmount: suspend (Double, String) -> Double?
) {
    val isPositive = balance.amount > BALANCE_THRESHOLD
    val isNegative = balance.amount < -BALANCE_THRESHOLD

    var convertedBalance by remember(balance.participant) { mutableStateOf<Double?>(null) }
    LaunchedEffect(balance.amount, showConversion) {
        convertedBalance = if (showConversion) convertAmount(balance.amount, EXPENSE_CURRENCY) else balance.amount
    }

    val (bgColor, amountColor, labelText) = when {
        isPositive -> BalanceStyle(
            bg = GreenBg50,
            amount = Green700,
            label = stringResource(R.string.balance_to_receive)
        )
        isNegative -> BalanceStyle(
            bg = RedBg50,
            amount = Red700,
            label = stringResource(R.string.balance_to_pay)
        )
        else -> BalanceStyle(
            bg = TealBg50,
            amount = Teal400,
            label = stringResource(R.string.balance_settled)
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

            val sign = when {
                isPositive -> "+"
                isNegative -> "-"
                else -> ""
            }
            val absAmount = if (isNegative) -balance.amount else balance.amount
            Column(horizontalAlignment = Alignment.End) {
                val displayAmount = if (showConversion && convertedBalance != null) {
                    val absConverted = if ((convertedBalance ?: 0.0) < 0) -(convertedBalance!!) else convertedBalance!!
                    sign + userFormat.format(absConverted)
                } else {
                    sign + originalFormat.format(absAmount)
                }
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                if (showConversion && convertedBalance != null) {
                    Text(
                        text = sign + originalFormat.format(absAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = amountColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private data class BalanceStyle(
    val bg: Color,
    val amount: Color,
    val label: String
)

@Composable
private fun ReimbursementRow(
    reimbursement: Reimbursement,
    originalFormat: NumberFormat,
    userFormat: NumberFormat,
    showConversion: Boolean,
    convertAmount: suspend (Double, String) -> Double?
) {
    var convertedAmount by remember(reimbursement.from + reimbursement.to) { mutableStateOf<Double?>(null) }
    LaunchedEffect(reimbursement.amount, showConversion) {
        convertedAmount = if (showConversion) convertAmount(reimbursement.amount, EXPENSE_CURRENCY) else reimbursement.amount
    }

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
            Text(
                text = reimbursement.from,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Red700,
                modifier = Modifier.weight(1f)
            )

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
                    val displayAmt = if (showConversion && convertedAmount != null) {
                        userFormat.format(convertedAmount)
                    } else {
                        originalFormat.format(reimbursement.amount)
                    }
                    Text(
                        text = displayAmt,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Teal400
                    )
                }
                if (showConversion && convertedAmount != null) {
                    Text(
                        text = originalFormat.format(reimbursement.amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

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
        GroupDetailScreen(group = group, onBack = {}, userCurrencyCode = "USD")
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
        BalancesTab(
            group = group,
            originalFormat = currencyFormat,
            userFormat = currencyFormat,
            showConversion = false,
            convertAmount = { amount, _ -> amount }
        )
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
        BalancesTab(
            group = group,
            originalFormat = currencyFormat,
            userFormat = currencyFormat,
            showConversion = false,
            convertAmount = { amount, _ -> amount }
        )
    }
}

@Composable
private fun ShareCodeCard(
    shareCode: String,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.share_code_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.share_code_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = shareCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Teal400,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onShareCode) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_code_title),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share))
                }
                TextButton(
                    onClick = onCopyCode
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_share_code_content_description),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.copy_share_code_button))
                }
            }
        }
    }
}
