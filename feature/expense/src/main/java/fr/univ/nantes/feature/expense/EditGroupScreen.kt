package fr.univ.nantes.feature.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
data class EditGroup(val groupId: Long)

private data class EditMemberField(val id: Int, val value: String, val isExisting: Boolean = false)

@Composable
fun EditGroupScreen(
    groupId: Long,
    viewModel: ExpenseViewModel,
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val group = state.groups.find { it.id == groupId }

    var groupName by remember { mutableStateOf("") }
    val nextId = remember { mutableStateOf(0) }
    val memberFields = remember { mutableStateListOf<EditMemberField>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val groupNameError = stringResource(R.string.error_group_name)
    val membersError = stringResource(R.string.error_members)
    val cannotRemoveMemberError = stringResource(R.string.error_cannot_remove_member_with_expenses)
    val duplicateMembersError = stringResource(R.string.error_duplicate_members)

    // Charger les données du groupe
    LaunchedEffect(group) {
        if (group != null && memberFields.isEmpty()) {
            groupName = group.groupName
            memberFields.addAll(
                group.participants.mapIndexed { index, name ->
                    EditMemberField(id = index, value = name, isExisting = true)
                }
            )
            nextId.value = group.participants.size
        }
    }

    if (group == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Green500)
        }
        return
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.edit_group),
                showBack = true,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.group_name_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                    errorMessage = null
                },
                label = {
                    Text(stringResource(R.string.group_name_label))
                },
                placeholder = {
                    Text(
                        stringResource(R.string.group_name_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = Green500,
                    focusedLabelColor = Green500,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    cursorColor = Green500
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                memberFields.forEachIndexed { index, member ->
                    key(member.id) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = member.value,
                                onValueChange = { newValue ->
                                    memberFields[index] = member.copy(value = newValue)
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.member_placeholder),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                enabled = !member.isExisting,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedBorderColor = Green500,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    cursorColor = Green500,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            if (memberFields.size > 2) {
                                IconButton(
                                    onClick = {
                                        val memberName = member.value
                                        val hasExpenses = group.expenses.any { it.paidBy == memberName }
                                        if (hasExpenses && member.isExisting) {
                                            errorMessage = cannotRemoveMemberError
                                        } else {
                                            memberFields.removeAt(index)
                                            errorMessage = null
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_member_description),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        memberFields.add(EditMemberField(id = nextId.value++, value = "", isExisting = false))
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
                        text = stringResource(R.string.edit_members),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Green500,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val members = memberFields.filter { it.value.isNotBlank() }
                        val normalizedMembers = members.map { it.copy(value = it.value.trim()) }
                        val uniqueNames = normalizedMembers.map { it.value }.distinct()

                        when {
                            groupName.isBlank() -> errorMessage = groupNameError
                            normalizedMembers.size < 2 -> errorMessage = membersError
                            uniqueNames.size < normalizedMembers.size -> {
                                errorMessage = duplicateMembersError
                            }
                            else -> {
                                val existingMembers = group.participants
                                val membersToAdd = uniqueNames.filter { it !in existingMembers }
                                val membersToRemove = existingMembers.filter { it !in uniqueNames }

                                val blockedRemovals = membersToRemove.filter { removedMember ->
                                    group.expenses.any { it.paidBy == removedMember }
                                }

                                if (blockedRemovals.isNotEmpty()) {
                                    blockedRemovals.forEach { blockedMember ->
                                        if (memberFields.none { it.value == blockedMember && it.isExisting }) {
                                            memberFields.add(
                                                EditMemberField(id = nextId.value++, value = blockedMember, isExisting = true)
                                            )
                                        }
                                    }
                                    errorMessage = cannotRemoveMemberError
                                } else {
                                    errorMessage = null
                                    val newName = if (groupName != group.groupName) groupName else null
                                    viewModel.updateGroup(groupId, newName, membersToAdd, membersToRemove)
                                    onBack()
                                }
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
                        text = stringResource(R.string.save_changes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
