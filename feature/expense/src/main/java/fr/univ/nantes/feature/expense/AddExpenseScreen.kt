package fr.univ.nantes.feature.expense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.Green500
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class AddExpenseRoute(val groupId: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    navigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val participants = state.participants

    val currencies = remember {
        listOf(
            "\u20AC EUR", "$ USD", "\u00A3 GBP", "\u00A5 JPY", "CHF", "$ CAD",
            "$ AUD", "\u00A5 CNY", "\u20B9 INR", "R$ BRL", "\u20BD RUB",
            "kr SEK", "kr NOK", "kr DKK", "z\u0142 PLN", "K\u010D CZK",
            "Ft HUF", "\u20BA TRY", "R ZAR", "\u20A9 KRW"
        )
    }
    var selectedCurrencyIndex by remember { mutableIntStateOf(0) }
    var currencyExpanded by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPayer by remember { mutableStateOf(if (participants.isNotEmpty()) participants[0] else "") }
    var payerExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedSplitType by remember { mutableStateOf(0) }
    val selectedParticipants = remember { mutableStateListOf<String>().apply { addAll(participants) } }
    val participantAmounts = remember { mutableStateMapOf<String, String>().apply { participants.forEach { put(it, "") } } }
    val participantShares = remember { mutableStateMapOf<String, String>().apply { participants.forEach { put(it, "") } } }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    val scrollState = rememberScrollState()
    val backgroundColor = Color(0xFFF2F2F2)
    val fieldShape = RoundedCornerShape(10.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color(0xFFDDDDDD),
        focusedBorderColor = Green500,
        cursorColor = Green500,
        unfocusedContainerColor = Color.White,
        focusedContainerColor = Color.White
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.new_expense_title),
                showBack = true,
                onBack = navigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .verticalScroll(scrollState)
                    .padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Titre de la depense
                Text(
                    text = stringResource(R.string.expense_title_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.expense_title_placeholder),
                            color = Color(0xFFBBBBBB)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    singleLine = true,
                    colors = fieldColors
                )

                // 2. Montant + Devise
                Text(
                    text = stringResource(R.string.amount_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
                            if (filtered.isEmpty() || filtered.any { it.isDigit() }) {
                                amount = filtered
                            }
                        },
                        placeholder = { Text("0.00", color = Color(0xFFBBBBBB)) },
                        modifier = Modifier.weight(2f),
                        shape = fieldShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors
                    )
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currencies[selectedCurrencyIndex],
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true,
                            colors = fieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.forEachIndexed { index, currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        selectedCurrencyIndex = index
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Date de la depense
                Text(
                    text = stringResource(R.string.expense_date_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = dateFormatter.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = fieldShape,
                    singleLine = true,
                    enabled = false,
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = stringResource(R.string.select_date),
                            tint = Green500
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFDDDDDD),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.White,
                        disabledTrailingIconColor = Green500
                    )
                )

                // 4. Ticket de caisse (optionnel)
                Text(
                    text = "Ticket de caisse (optionnel)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { /* TODO: Implement camera action */ },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFDDDDDD), fieldShape),
                        shape = fieldShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = "Camera",
                            modifier = Modifier.padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Prendre une photo", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = { /* TODO: Implement file picker action */ },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFDDDDDD), fieldShape),
                        shape = fieldShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Choisir", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // 5. Paye par
                Text(
                    text = stringResource(R.string.paid_by_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPayer,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.paid_by_label), color = Color(0xFFBBBBBB)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        participants.forEach { participant ->
                            DropdownMenuItem(
                                text = { Text(participant) },
                                onClick = {
                                    selectedPayer = participant
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                // 6. Type de partage
                Text(
                    text = stringResource(R.string.split_type_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val splitLabels = listOf(
                        stringResource(R.string.split_equally),
                        stringResource(R.string.split_by_share),
                        stringResource(R.string.split_by_amount)
                    )
                    splitLabels.forEachIndexed { index, label ->
                        val isSelected = selectedSplitType == index
                        Button(
                            onClick = { selectedSplitType = index },
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (!isSelected) Modifier.border(1.dp, Color(0xFFDDDDDD), fieldShape)
                                    else Modifier
                                ),
                            shape = fieldShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Green500 else Color.White,
                                contentColor = if (isSelected) Color.White else Color.Gray
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 7. Participants
                Text(
                    text = stringResource(R.string.participants_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        participants.forEach { participant ->
                            val isChecked = participant in selectedParticipants
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedParticipants.remove(participant)
                                        else selectedParticipants.add(participant)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedParticipants.add(participant)
                                        else selectedParticipants.remove(participant)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Green500,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = participant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                selectedParticipants.clear()
                                selectedParticipants.addAll(participants)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                stringResource(R.string.select_all),
                                color = Green500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 8a. Parts par participant si "By Share" est sélectionné
                if (selectedSplitType == 1) {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val totalShares = selectedParticipants.sumOf {
                        participantShares[it]?.toDoubleOrNull() ?: 1.0
                    }
                    Text(
                        text = "Parts par participant",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            selectedParticipants.forEach { participant ->
                                val shares = participantShares[participant]?.toDoubleOrNull() ?: 1.0
                                val computed = if (totalShares > 0) shares / totalShares * amountValue else 0.0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = participant,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedTextField(
                                        value = participantShares[participant] ?: "",
                                        onValueChange = { newValue ->
                                            val filtered = buildString {
                                                for (c in newValue) {
                                                    if (c.isDigit()) append(c)
                                                }
                                            }
                                            if (filtered.isEmpty() || filtered.any { it.isDigit() }) {
                                                participantShares[participant] = filtered
                                            }
                                        },
                                        placeholder = { Text("1", color = Color(0xFFBBBBBB)) },
                                        modifier = Modifier.weight(1f),
                                        shape = fieldShape,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = fieldColors,
                                        suffix = { Text("part(s)", color = Color.Gray) }
                                    )
                                    Text(
                                        text = "= ${String.format("%.2f", computed)}${currencies[selectedCurrencyIndex].split(" ")[0]}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Green500,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 8b. Montants par participant si "Par montant" est sélectionné
                if (selectedSplitType == 2) {
                    Text(
                        text = "Montants par participant",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            selectedParticipants.forEach { participant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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
                                                participantAmounts[participant] = filtered
                                            }
                                        },
                                        placeholder = { Text("0", color = Color(0xFFBBBBBB)) },
                                        modifier = Modifier.weight(1f),
                                        shape = fieldShape,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = fieldColors,
                                        suffix = { Text(currencies[selectedCurrencyIndex].split(" ")[0], color = Color.Gray) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. Bouton Ajouter
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amountValue > 0 && selectedPayer.isNotBlank()) {
                            val splitDetailsMap: Map<String, Double> = when (selectedSplitType) {
                                1 -> {
                                    val totalShares = selectedParticipants.sumOf {
                                        participantShares[it]?.toDoubleOrNull() ?: 1.0
                                    }
                                    selectedParticipants.associate { p ->
                                        val shares = participantShares[p]?.toDoubleOrNull() ?: 1.0
                                        p to if (totalShares > 0) shares / totalShares * amountValue else 0.0
                                    }
                                }
                                2 -> selectedParticipants.associate { p ->
                                    p to (participantAmounts[p]?.toDoubleOrNull() ?: 0.0)
                                }
                                else -> emptyMap()
                            }
                            viewModel.addExpense(
                                description = title,
                                amount = amountValue,
                                paidBy = selectedPayer,
                                splitType = selectedSplitType,
                                splitDetails = splitDetailsMap
                            )
                            navigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green500),
                    enabled = title.isNotBlank() && amount.isNotBlank() && selectedPayer.isNotBlank()
                ) {
                    Text(
                        stringResource(R.string.add_expense_button),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scrollbar droite
            val maxScroll = scrollState.maxValue.toFloat()
            if (maxScroll > 0f) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(16.dp)
                ) {
                    val barWidth = 4.dp.toPx()
                    val barX = 6.dp.toPx()
                    val thumbHeight = size.height * size.height / (size.height + maxScroll)
                    val thumbTop = scrollState.value.toFloat() / maxScroll * (size.height - thumbHeight)

                    // Track
                    drawRoundRect(
                        color = Green500.copy(alpha = 0.2f),
                        topLeft = Offset(barX, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                    // Thumb
                    drawRoundRect(
                        color = Green500,
                        topLeft = Offset(barX, thumbTop),
                        size = Size(barWidth, thumbHeight),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm), color = Green500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
