package fr.univ.nantes.feature.expense

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.EuroSymbol
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.AppliCashTheme
import fr.univ.nantes.core.ui.Green500
import fr.univ.nantes.core.ui.GreenBg50
import fr.univ.nantes.core.ui.Teal600
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data class AddExpenseRoute(val groupId: Long)

private val avatarColors = listOf(
    Color(0xFF6366F1),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF8B5CF6),
    Color(0xFF06B6D4),
    Color(0xFFEC4899),
    Color(0xFF14B8A6),
    Color(0xFFF97316)
)
private val quickAmounts = listOf(5.0, 10.0, 20.0, 50.0, 100.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    navigateBack: () -> Unit,
    onStartCamera: () -> Unit = {},
    receiptPreviewPath: String? = null,
    onClearReceipt: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val participants = state.participants

    val fallbackCurrencies = remember {
        listOf(
            "EUR", "USD", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "INR", "BRL",
            "RUB", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "TRY", "ZAR", "KRW"
        )
    }
    val availableCurrencies = state.availableCurrencies.ifEmpty { fallbackCurrencies }

    val userCurrency = state.userCurrencyCode
    var selectedCurrency by remember(userCurrency) {
        mutableStateOf(userCurrency.ifBlank { "EUR" })
    }
    var currencyExpanded by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val defaultPayer = remember(state.currentUserName, participants) {
        val userName = state.currentUserName ?: ""
        participants.firstOrNull { it.equals(userName, ignoreCase = true) }
            ?: participants.firstOrNull() ?: ""
    }
    var selectedPayer by remember(defaultPayer) { mutableStateOf(defaultPayer) }
    var payerExpanded by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var datePickerOpen by remember { mutableStateOf(false) }
    val closeDatePicker = { datePickerOpen = false }
    var selectedSplitType by remember { mutableStateOf(0) }
    val selectedParticipants = remember { mutableStateListOf<String>() }
    val participantShares = remember { mutableStateMapOf<String, Int>() }
    val participantAmounts = remember { mutableStateMapOf<String, String>() }
    val lockedAmounts = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(participants) {
        if (selectedPayer.isEmpty() && participants.isNotEmpty()) {
            selectedPayer = participants[0]
        }
        val currentSelected = selectedParticipants.toSet()
        val newParticipants = participants.toSet()
        selectedParticipants.removeAll { it !in newParticipants }
        participants.forEach { p ->
            if (p !in currentSelected) {
                selectedParticipants.add(p)
                participantShares.putIfAbsent(p, 1)
                participantAmounts.putIfAbsent(p, "")
            }
        }
        participantShares.keys.retainAll(newParticipants)
        participantAmounts.keys.retainAll(newParticipants)
        lockedAmounts.keys.retainAll(newParticipants)
    }

    LaunchedEffect(selectedSplitType) {
        if (selectedSplitType == 2) {
            lockedAmounts.clear()
            participantAmounts.keys.forEach { participantAmounts[it] = "" }
        }
    }

    val amountValue = amount.toDoubleOrNull() ?: 0.0
    LaunchedEffect(amountValue, selectedCurrency) {
        viewModel.updateLiveConversion(amountValue, selectedCurrency)
    }
    val convertedInBase = state.convertedAmountInBase

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val scrollState = rememberScrollState()
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = Green500,
        cursorColor = Green500,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface
    )

    val isFormValid = title.isNotBlank() && amountValue > 0 && selectedPayer.isNotBlank()

    val snackbarHostState = remember { SnackbarHostState() }
    val photoQualityLowMsg = stringResource(R.string.photo_quality_low)
    LaunchedEffect(receiptPreviewPath) {
        receiptPreviewPath?.let { path ->
            val file = File(path)
            if (file.exists() && !ReceiptPhotoHelper.isPhotoQualityAcceptable(file)) {
                snackbarHostState.showSnackbar(photoQualityLowMsg)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.new_expense_title),
                showBack = true,
                onBack = navigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488))))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (amountValue > 0) "$selectedCurrency ${"%.2f".format(amountValue)}" else "– –",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 36.sp
                    )
                    if (convertedInBase != null && selectedCurrency != ExpenseViewModel.STORAGE_CURRENCY) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CurrencyExchange,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.converted_amount, ExpenseViewModel.STORAGE_CURRENCY, "%.2f".format(convertedInBase)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    if (title.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(icon = Icons.Outlined.ShoppingCart, title = stringResource(R.string.expense_title_label)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.expense_title_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SectionCard(icon = Icons.Outlined.EuroSymbol, title = stringResource(R.string.amount_label)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                                append(c)
                                                dotSeen = true
                                            }
                                        }
                                    }
                                }
                                if (filtered.isEmpty() || filtered.any { it.isDigit() }) amount = filtered
                            },
                            placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.weight(2f),
                            shape = fieldShape,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedCurrency,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (currencyExpanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currencyExpanded = !currencyExpanded },
                                shape = fieldShape,
                                singleLine = true,
                                colors = fieldColors,
                                enabled = false
                            )
                            DropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                availableCurrencies.forEach { currency ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(currency, fontWeight = FontWeight.SemiBold)
                                                if (currency == userCurrency) {
                                                    Surface(
                                                        color = GreenBg50,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            stringResource(R.string.my_currency_label),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Green500,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCurrency = currency
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.forEach { preset ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { amount = preset.toInt().toString() },
                                color = if (amount == preset.toInt().toString()) {
                                    GreenBg50
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = if (amount == preset.toInt().toString()) {
                                    androidx.compose.foundation.BorderStroke(1.dp, Green500)
                                } else {
                                    null
                                }
                            ) {
                                Text(
                                    text = preset.toInt().toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (amount == preset.toInt().toString()) {
                                        Green500
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SectionCard(icon = Icons.Outlined.CalendarMonth, title = stringResource(R.string.expense_date_label)) {
                            OutlinedTextField(
                                value = dateFormatter.format(Date(selectedDate)),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { datePickerOpen = true },
                                shape = fieldShape,
                                singleLine = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Green500)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledTrailingIconColor = Green500
                                )
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SectionCard(icon = Icons.Outlined.Person, title = stringResource(R.string.paid_by_label)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedPayer,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (payerExpanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { payerExpanded = !payerExpanded },
                                    shape = fieldShape,
                                    singleLine = true,
                                    colors = fieldColors,
                                    enabled = false,
                                    leadingIcon = {
                                        if (selectedPayer.isNotBlank()) {
                                            val idx = participants.indexOf(selectedPayer).coerceAtLeast(0)
                                            ParticipantAvatar(
                                                name = selectedPayer,
                                                color = avatarColors[idx % avatarColors.size],
                                                size = 24
                                            )
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = payerExpanded,
                                    onDismissRequest = { payerExpanded = false }
                                ) {
                                    participants.forEachIndexed { idx, participant ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    ParticipantAvatar(
                                                        name = participant,
                                                        color = avatarColors[idx % avatarColors.size],
                                                        size = 26
                                                    )
                                                    Text(participant)
                                                    if (participant.equals(state.currentUserName, ignoreCase = true)) {
                                                        Spacer(Modifier.weight(1f))
                                                        Surface(
                                                            color = GreenBg50,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                stringResource(R.string.me_label),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Green500,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPayer = participant
                                                payerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SectionCard(icon = Icons.Outlined.Splitscreen, title = stringResource(R.string.split_type_label)) {
                    val splitLabels = listOf(
                        stringResource(R.string.split_equally),
                        stringResource(R.string.split_by_share),
                        stringResource(R.string.split_by_amount)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(3.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            splitLabels.forEachIndexed { index, label ->
                                val isSelected = selectedSplitType == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF10B981), Color(0xFF0D9488))
                                                )
                                            } else {
                                                Brush.horizontalGradient(
                                                    listOf(Color.Transparent, Color.Transparent)
                                                )
                                            }
                                        )
                                        .clickable { selectedSplitType = index }
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                SectionCard(icon = Icons.Outlined.Group, title = stringResource(R.string.participants_label)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        participants.forEachIndexed { idx, participant ->
                            val isChecked = participant in selectedParticipants
                            val avatarColor = avatarColors[idx % avatarColors.size]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecked) GreenBg50 else Color.Transparent)
                                    .clickable {
                                        if (isChecked) {
                                            selectedParticipants.remove(participant)
                                        } else if (participant !in selectedParticipants) {
                                            selectedParticipants.add(participant)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ParticipantAvatar(name = participant, color = avatarColor, size = 32)
                                Text(
                                    text = participant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isChecked) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Green500, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                }
                            }
                            if (idx < participants.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            selectedParticipants.clear()
                            selectedParticipants.addAll(participants)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.select_all), color = Green500, fontWeight = FontWeight.SemiBold)
                    }
                }

                AnimatedVisibility(
                    visible = selectedSplitType == 1,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val totalShares = selectedParticipants.sumOf { (participantShares[it] ?: 1).toDouble() }
                    SectionCard(
                        icon = Icons.Outlined.Splitscreen,
                        title = stringResource(R.string.shares_per_participant_label)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedParticipants.forEachIndexed { _, participant ->
                                val share = participantShares[participant] ?: 1
                                val computed = if (totalShares > 0) share / totalShares * amountValue else 0.0
                                val avatarColor = avatarColors[
                                    participants.indexOf(participant).coerceAtLeast(0) % avatarColors.size
                                ]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ParticipantAvatar(name = participant, color = avatarColor, size = 30)
                                    Text(
                                        text = participant,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            )
                                            .clickable {
                                                val current = participantShares[participant] ?: 1
                                                if (current > 1) participantShares[participant] = current - 1
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Remove,
                                            contentDescription = stringResource(R.string.decrease_share),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(GreenBg50, RoundedCornerShape(8.dp))
                                            .border(1.dp, Green500.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = share.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Green500
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Green500, CircleShape)
                                            .clickable {
                                                participantShares[participant] = (participantShares[participant] ?: 1) + 1
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = stringResource(R.string.increase_share),
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Surface(
                                        color = GreenBg50,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$selectedCurrency ${"%.2f".format(computed)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Teal600,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedSplitType == 2,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val totalAssigned = selectedParticipants.sumOf {
                        participantAmounts[it]?.toDoubleOrNull() ?: 0.0
                    }
                    val remaining = amountValue - totalAssigned
                    val equalShare = if (selectedParticipants.isNotEmpty()) {
                        amountValue / selectedParticipants.size
                    } else {
                        0.0
                    }

                    SectionCard(
                        icon = Icons.Outlined.Splitscreen,
                        title = stringResource(R.string.amounts_per_participant_label)
                    ) {
                        if (amountValue > 0 && selectedParticipants.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    lockedAmounts.clear()
                                    selectedParticipants.forEach { p ->
                                        participantAmounts[p] = "%.2f".format(equalShare)
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    stringResource(R.string.split_equally_quick, selectedCurrency, "%.2f".format(equalShare)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Green500
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedParticipants.forEachIndexed { _, participant ->
                                val avatarColor = avatarColors[
                                    participants.indexOf(participant).coerceAtLeast(0) % avatarColors.size
                                ]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ParticipantAvatar(name = participant, color = avatarColor, size = 30)
                                    Text(
                                        text = participant,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedTextField(
                                        value = participantAmounts[participant] ?: "",
                                        onValueChange = { newValue ->
                                            var dotSeen = false
                                            var hasDigitBeforeDot = false
                                            val filtered = buildString {
                                                for (c in newValue) {
                                                    when {
                                                        c.isDigit() -> {
                                                            append(c)
                                                            if (!dotSeen) hasDigitBeforeDot = true
                                                        }
                                                        c == '.' && !dotSeen && hasDigitBeforeDot -> {
                                                            append(c)
                                                            dotSeen = true
                                                        }
                                                    }
                                                }
                                            }
                                            if (filtered.isEmpty() || filtered.any { it.isDigit() }) {
                                                if (filtered.isEmpty()) {
                                                    lockedAmounts.remove(participant)
                                                    participantAmounts[participant] = ""
                                                } else {
                                                    lockedAmounts[participant] = true
                                                    participantAmounts[participant] = filtered

                                                    val lockedTotal = selectedParticipants
                                                        .filter { lockedAmounts[it] == true }
                                                        .sumOf { participantAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                                                    val freeParticipants = selectedParticipants
                                                        .filter { lockedAmounts[it] != true }
                                                    val remainingForFree = (amountValue - lockedTotal).coerceAtLeast(0.0)
                                                    if (freeParticipants.isNotEmpty()) {
                                                        val share = remainingForFree / freeParticipants.size
                                                        freeParticipants.forEach { other ->
                                                            participantAmounts[other] = "%.2f".format(share)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "%.2f".format(equalShare),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.width(130.dp),
                                        shape = fieldShape,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = fieldColors,
                                        suffix = {
                                            Text(
                                                selectedCurrency,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }
                            }
                            if (amountValue > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.remaining_to_split),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (kotlin.math.abs(remaining) > 0.01) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            Teal600
                                        }
                                    )
                                    Surface(
                                        color = if (kotlin.math.abs(remaining) > 0.01) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            GreenBg50
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$selectedCurrency ${"%.2f".format(remaining)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (kotlin.math.abs(remaining) > 0.01) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                Teal600
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SectionCard(icon = Icons.Outlined.Receipt, title = stringResource(R.string.receipt_section_label)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartCamera() },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green500,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.take_photo),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = { /* TODO: file picker */ },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.choose_file),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Preview + retake/remove
                    receiptPreviewPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(path)
                            if (bitmap != null) {
                                Spacer(Modifier.height(8.dp))
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = stringResource(R.string.receipt_preview), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(160.dp))
                            } else {
                                 Text(text = path, style = MaterialTheme.typography.bodySmall)
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = { onStartCamera() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                                ) {
                                    Text(stringResource(R.string.retake_photo))
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onClearReceipt,
                                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                                ) {
                                    Text(stringResource(R.string.remove_photo))
                                }
                            }
                        }
                    }

                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (isFormValid) {
                            val splitDetailsMap: Map<String, Double> = when (selectedSplitType) {
                                1 -> {
                                    val totalShares = selectedParticipants.sumOf {
                                        (participantShares[it] ?: 1).toDouble()
                                    }
                                    selectedParticipants.associateWith { p ->
                                        val shares = (participantShares[p] ?: 1).toDouble()
                                        if (totalShares > 0) shares / totalShares * amountValue else 0.0
                                    }
                                }
                                2 -> selectedParticipants.associateWith { p -> (participantAmounts[p]?.toDoubleOrNull() ?: 0.0) }
                                else -> emptyMap()
                            }
                            // Ensure payer exists in the ViewModel state participants list.
                            // If not present, add it locally so the ViewModel's addExpense
                            // validation (which requires the payer to be a participant) passes.
                            if (!state.participants.contains(selectedPayer)) {
                                viewModel.addParticipant(selectedPayer)
                            }

                            viewModel.addExpense(
                                description = title,
                                amount = amountValue,
                                paidBy = selectedPayer,
                                splitType = selectedSplitType,
                                splitDetails = splitDetailsMap,
                                receiptPath = receiptPreviewPath
                            )
                            navigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormValid) {
                            Green500
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isFormValid) 4.dp else 0.dp
                    ),
                    enabled = isFormValid
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.add_expense_button),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (datePickerOpen) {
        ExpenseDatePickerDialog(
            initialDateMillis = selectedDate,
            onConfirm = { millis ->
                selectedDate = millis
                closeDatePicker()
            },
            onDismiss = closeDatePicker
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePickerDialog(
    initialDateMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(datePickerState.selectedDateMillis ?: initialDateMillis)
            }) { Text(stringResource(R.string.confirm), color = Green500) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) { DatePicker(state = datePickerState) }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GreenBg50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Green500, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ParticipantAvatar(name: String, color: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color.copy(alpha = 0.15f), CircleShape)
            .border(1.5.dp, color.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = (size * 0.38).sp
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private fun previewState(
    participants: List<String> = listOf("Alice", "Bob", "Charlie"),
    currentUserName: String = "Alice",
    userCurrencyCode: String = "EUR",
    availableCurrencies: List<String> = listOf("EUR", "USD", "GBP", "JPY", "CHF"),
    convertedAmountInBase: Double? = null
) = ExpenseState(
    groupName = "Summer trip",
    participants = participants,
    currentUserName = currentUserName,
    isLoggedIn = true,
    userCurrencyCode = userCurrencyCode,
    availableCurrencies = availableCurrencies,
    convertedAmountInBase = convertedAmountInBase
)

@Preview(name = "Add Expense – Light", showBackground = true, showSystemUi = true)
@Composable
private fun AddExpenseScreenPreview() {
    AppliCashTheme {
        AddExpenseScreenContent(state = previewState())
    }
}

@Preview(
    name = "Add Expense – Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AddExpenseScreenDarkPreview() {
    AppliCashTheme(darkTheme = true) {
        AddExpenseScreenContent(state = previewState())
    }
}

@Preview(name = "Add Expense – With conversion", showBackground = true, showSystemUi = true)
@Composable
private fun AddExpenseScreenConversionPreview() {
    AppliCashTheme {
        AddExpenseScreenContent(
            state = previewState(
                userCurrencyCode = "EUR",
                availableCurrencies = listOf("EUR", "USD", "GBP"),
                convertedAmountInBase = 42.50
            ),
            previewAmount = "45",
            previewCurrency = "USD"
        )
    }
}

@Preview(name = "Add Expense – Split by share", showBackground = true, showSystemUi = true)
@Composable
private fun AddExpenseScreenSharePreview() {
    AppliCashTheme {
        AddExpenseScreenContent(
            state = previewState(),
            previewAmount = "120",
            previewSplitType = 1
        )
    }
}

@Preview(name = "Add Expense – Split by amount", showBackground = true, showSystemUi = true)
@Composable
private fun AddExpenseScreenAmountSplitPreview() {
    AppliCashTheme {
        AddExpenseScreenContent(
            state = previewState(),
            previewAmount = "90",
            previewSplitType = 2
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseScreenContent(
    state: ExpenseState,
    previewAmount: String = "",
    previewCurrency: String = state.userCurrencyCode,
    previewSplitType: Int = 0
) {
    val participants = state.participants
    val selectedCurrency = previewCurrency
    val amount = previewAmount
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val convertedInBase = state.convertedAmountInBase
    val title = if (previewAmount.isNotEmpty()) "Restaurant" else ""
    val selectedPayer = state.currentUserName ?: participants.firstOrNull() ?: ""
    val selectedDate = System.currentTimeMillis()
    val selectedSplitType = previewSplitType
    val selectedParticipants = participants
    val participantShares = participants.associateWith { 1 }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = Green500,
        cursorColor = Green500,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface
    )
    val isFormValid = title.isNotBlank() && amountValue > 0 && selectedPayer.isNotBlank()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.new_expense_title),
                showBack = true,
                onBack = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488))))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (amountValue > 0) "$selectedCurrency ${"%.2f".format(amountValue)}" else "– –",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 36.sp
                    )
                    if (convertedInBase != null && selectedCurrency != ExpenseViewModel.STORAGE_CURRENCY) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CurrencyExchange,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.converted_amount, ExpenseViewModel.STORAGE_CURRENCY, "%.2f".format(convertedInBase)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    if (title.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(icon = Icons.Outlined.ShoppingCart, title = stringResource(R.string.expense_title_label)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {},
                        placeholder = {
                            Text(
                                stringResource(R.string.expense_title_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                SectionCard(icon = Icons.Outlined.EuroSymbol, title = stringResource(R.string.amount_label)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {},
                            placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.weight(2f),
                            shape = fieldShape,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            shape = fieldShape,
                            singleLine = true,
                            colors = fieldColors
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickAmounts.forEach { preset ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {},
                                color = if (amount == preset.toInt().toString()) {
                                    GreenBg50
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = if (amount == preset.toInt().toString()) {
                                    androidx.compose.foundation.BorderStroke(1.dp, Green500)
                                } else {
                                    null
                                }
                            ) {
                                Text(
                                    text = preset.toInt().toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (amount == preset.toInt().toString()) {
                                        Green500
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SectionCard(icon = Icons.Outlined.CalendarMonth, title = stringResource(R.string.expense_date_label)) {
                            OutlinedTextField(
                                value = dateFormatter.format(Date(selectedDate)),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = fieldShape,
                                singleLine = true,
                                enabled = false,
                                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = Green500) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledTrailingIconColor = Green500
                                )
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SectionCard(icon = Icons.Outlined.Person, title = stringResource(R.string.paid_by_label)) {
                            OutlinedTextField(
                                value = selectedPayer,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = fieldShape,
                                singleLine = true,
                                colors = fieldColors,
                                leadingIcon = {
                                    ParticipantAvatar(
                                        name = selectedPayer,
                                        color = avatarColors[0],
                                        size = 24
                                    )
                                }
                            )
                        }
                    }
                }

                SectionCard(icon = Icons.Outlined.Splitscreen, title = stringResource(R.string.split_type_label)) {
                    val splitLabels = listOf(
                        stringResource(R.string.split_equally),
                        stringResource(R.string.split_by_share),
                        stringResource(R.string.split_by_amount)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(3.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            splitLabels.forEachIndexed { index, label ->
                                val isSelected = selectedSplitType == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488)))
                                            } else {
                                                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                            }
                                        )
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                SectionCard(icon = Icons.Outlined.Group, title = stringResource(R.string.participants_label)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        participants.forEachIndexed { idx, participant ->
                            val isChecked = participant in selectedParticipants
                            val avatarColor = avatarColors[idx % avatarColors.size]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecked) GreenBg50 else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ParticipantAvatar(name = participant, color = avatarColor, size = 32)
                                Text(
                                    text = participant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isChecked) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Green500, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                }
                            }
                            if (idx < participants.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                    TextButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.select_all), color = Green500, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (selectedSplitType == 1) {
                    val totalShares = selectedParticipants.sumOf { (participantShares[it] ?: 1).toDouble() }
                    SectionCard(icon = Icons.Outlined.Splitscreen, title = stringResource(R.string.shares_per_participant_label)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedParticipants.forEachIndexed { _, participant ->
                                val share = participantShares[participant] ?: 1
                                val computed = if (totalShares > 0) share / totalShares * amountValue else 0.0
                                val avatarColor = avatarColors[participants.indexOf(participant).coerceAtLeast(0) % avatarColors.size]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ParticipantAvatar(name = participant, color = avatarColor, size = 30)
                                    Text(text = participant, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Remove, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                    Box(modifier = Modifier.size(36.dp).background(GreenBg50, RoundedCornerShape(8.dp)).border(1.dp, Green500.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Text(text = share.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Green500)
                                    }
                                    Box(modifier = Modifier.size(32.dp).background(Green500, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Surface(
                                        color = GreenBg50,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(text = "$selectedCurrency ${"%.2f".format(computed)}", style = MaterialTheme.typography.labelMedium, color = Teal600, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedSplitType == 2) {
                    val equalShare = if (selectedParticipants.isNotEmpty()) amountValue / selectedParticipants.size else 0.0
                    SectionCard(icon = Icons.Outlined.Splitscreen, title = stringResource(R.string.amounts_per_participant_label)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedParticipants.forEachIndexed { _, participant ->
                                val avatarColor = avatarColors[participants.indexOf(participant).coerceAtLeast(0) % avatarColors.size]
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ParticipantAvatar(name = participant, color = avatarColor, size = 30)
                                    Text(text = participant, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = "%.2f".format(equalShare),
                                        onValueChange = {},
                                        modifier = Modifier.width(110.dp),
                                        shape = fieldShape,
                                        singleLine = true,
                                        colors = fieldColors,
                                        suffix = { Text(selectedCurrency, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormValid) Green500 else MaterialTheme.colorScheme.outlineVariant,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isFormValid) 4.dp else 0.dp),
                    enabled = isFormValid
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_expense_button), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
