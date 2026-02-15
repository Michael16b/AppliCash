package fr.univ.nantes.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.feature.expense.ExpenseViewModel
import fr.univ.nantes.feature.expense.GroupData
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.Serializable

// TODO: These imports create tight coupling between features. Consider moving GroupData
// and ExpenseViewModel interface/state to a shared domain module to improve modularity.

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel? = null,
    onAddGroupClick: () -> Unit = {},
    onGroupClick: (GroupData) -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val groups = viewModel?.state?.collectAsState()?.value?.groups ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488))))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = stringResource(R.string.profile),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGroupClick,
                containerColor = Color(0xFF00BFA5),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_group),
                    tint = Color.White
                )
            }
        }
    ) { innerPadding ->
        if (groups.isEmpty()) {
            EmptyGroupsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            GroupsList(
                groups = groups,
                onGroupClick = onGroupClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
fun EmptyGroupsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_groups_created),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.press_plus_to_create_group),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GroupsList(
    groups: List<GroupData>,
    onGroupClick: (GroupData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups, key = { it.id }) { group ->
            GroupCard(
                group = group,
                onClick = { onGroupClick(group) }
            )
        }
    }
}

@Composable
fun GroupCard(
    group: GroupData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = group.expenses.sumOf { it.amount }
    val isDarkMode = isSystemInDarkTheme()
    val cardColor = if (isDarkMode) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White
    }

    val currencyFormat = remember {
        // TODO: Make currency configurable per group or user preference
        // Currently hardcoded to EUR for all users
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance("EUR")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = group.groupName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            modifier = Modifier.padding(0.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${group.participants.size} ${stringResource(if (group.participants.size != 1) R.string.members else R.string.member)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Receipt,
                            contentDescription = null,
                            modifier = Modifier.padding(0.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${group.expenses.size} ${stringResource(if (group.expenses.size != 1) R.string.expenses else R.string.expense)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currencyFormat.format(totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00BFA5)
                )
            }
        }
    }
}

@Serializable
data class Home(val username: String? = null)

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreview() {
    AppliCashTheme {
        HomeScreen(
            viewModel = null,
            onAddGroupClick = {},
            onGroupClick = {},
            onProfileClick = {}
        )
//        GroupCard(
//            group = GroupData(
//                id = 1L,
//                groupName = "Group 1",
//                participants = listOf("Alice", "Bob"),
//                expenses = listOf(Expense("Expense 1", 5.0, "Alice"), Expense("Expense 2", 10.0, "Bob"))
//            ),
//            onClick = {}
//        )
    }
}
