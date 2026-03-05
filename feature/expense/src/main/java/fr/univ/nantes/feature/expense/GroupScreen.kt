package fr.univ.nantes.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.univ.nantes.core.ui.AppTopBar
import fr.univ.nantes.core.ui.Green500
import kotlinx.serialization.Serializable

@Serializable
data object Group

@Composable
fun GroupScreen(
    viewModel: ExpenseViewModel,
    navigateToHome: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val memberFields = remember { mutableStateListOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val groupNameError = stringResource(R.string.error_group_name)
    val membersError = stringResource(R.string.error_members)

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.new_group),
                showBack = true,
                onBack = navigateToHome
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // --- Nom du groupe ---
            Text(
                text = stringResource(R.string.group_name_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = state.groupName,
                onValueChange = { viewModel.setGroupName(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.group_name_placeholder),
                        color = Color(0xFFBBBBBB)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedBorderColor = Green500,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    cursorColor = Green500
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Membres ---
            Text(
                text = stringResource(R.string.members_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Champs des membres ---
                memberFields.forEachIndexed { index, value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { memberFields[index] = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.member_placeholder),
                                    color = Color(0xFFBBBBBB)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedBorderColor = Green500,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                cursorColor = Green500
                            )
                        )

                        if (memberFields.size > 1) {
                            IconButton(
                                onClick = { memberFields.removeAt(index) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_participant_description),
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- Message d'erreur ---
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // --- Bouton Ajouter un membre ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        memberFields.add("")
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_participant_description),
                        tint = Green500,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = stringResource(R.string.add_member),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Green500,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Bouton Créer groupe ---
                Button(
                    onClick = {
                        val members = memberFields.filter { it.isNotBlank() }
                        when {
                            state.groupName.isBlank() -> errorMessage = groupNameError
                            members.size < 2 -> errorMessage = membersError
                            else -> {
                                errorMessage = null
                                members.forEach { viewModel.addParticipant(it) }
                                viewModel.saveGroup()
                                navigateToHome()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green500,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_group_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
