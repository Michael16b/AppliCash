package fr.univ.nantes.feature.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
data object Group

@Composable
fun GroupScreen(
    viewModel: ExpenseViewModel,
    navigateToExpense: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var participantInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Nouveau Groupe",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.groupName,
            onValueChange = { viewModel.setGroupName(it) },
            label = { Text("Nom du groupe") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = participantInput,
                onValueChange = { participantInput = it },
                label = { Text("Participant") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    viewModel.addParticipant(participantInput)
                    participantInput = ""
                }
            ) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Participants (${state.participants.size})",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(state.participants) { participant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(participant)
                    TextButton(onClick = { viewModel.removeParticipant(participant) }) {
                        Text("X")
                    }
                }
            }
        }

        Button(
            onClick = navigateToExpense,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.groupName.isNotBlank() && state.participants.size >= 2
        ) {
            Text("Creer le groupe")
        }
    }
}
