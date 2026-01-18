package fr.univ.nantes.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
data object Group

@Composable
fun GroupScreen(
    viewModel: ExpenseViewModel,
    navigateToExpense: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var participantInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.new_group),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.groupName,
            onValueChange = { viewModel.setGroupName(it) },
            label = { Text(stringResource(R.string.group_name)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = participantInput,
                onValueChange = { participantInput = it },
                label = { Text(stringResource(R.string.participant)) },
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (participantInput.isNotBlank()) {
                        viewModel.addParticipant(participantInput)
                        participantInput = ""
                    }
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_participant_description))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.participants_count, state.participants.size),
            style = MaterialTheme.typography.titleMedium,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(state.participants) { participant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(participant)
                    IconButton(onClick = { viewModel.removeParticipant(participant) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_participant_description))
                    }
                }
            }
        }

        Button(
            onClick = navigateToExpense,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.groupName.isNotBlank() && state.participants.size >= 2,
        ) {
            Text(stringResource(R.string.continue_button))
        }
    }
}
