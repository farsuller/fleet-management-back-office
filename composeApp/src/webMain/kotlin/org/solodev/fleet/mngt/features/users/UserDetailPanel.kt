package org.solodev.fleet.mngt.features.users

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.solodev.fleet.mngt.api.dto.auth.UserDto
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun UserDetailPanel(
    selectedUser: UserDto?,
    onClose: () -> Unit,
    viewModel: UsersViewModel,
) {
    val colors = fleetColors
    val roles by viewModel.roles.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionResult) {
        actionResult?.onFailure {
            errorMessage = it.message ?: "Action failed"
            viewModel.clearActionResult()
        } ?: run {
            errorMessage = null
        }
    }

    AnimatedVisibility(
        visible = selectedUser != null,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().width(450.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.surface,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                // State for editing
                var firstName by remember(selectedUser) { mutableStateOf(selectedUser?.firstName ?: "") }
                var lastName by remember(selectedUser) { mutableStateOf(selectedUser?.lastName ?: "") }
                var phone by remember(selectedUser) { mutableStateOf(selectedUser?.phone ?: "") }
                var department by remember(selectedUser) {
                    mutableStateOf(
                        selectedUser?.staffProfile?.department ?: "",
                    )
                }
                var position by remember(selectedUser) { mutableStateOf(selectedUser?.staffProfile?.position ?: "") }
                var statusIsActive by remember(selectedUser) { mutableStateOf(selectedUser?.isActive ?: true) }

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "User Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.text1,
                        )
                        Text(
                            text = selectedUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text2,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                errorMessage?.let {
                    Surface(
                        color = colors.cancelled.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    ) {
                        Text(
                            it,
                            color = colors.cancelled,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Edit Fields
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Position") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Switch(checked = statusIsActive, onCheckedChange = { statusIsActive = it })
                    Spacer(Modifier.width(12.dp))
                    Text("User is Active", color = colors.text1)
                }

                Button(
                    onClick = {
                        viewModel.updateUser(
                            id = selectedUser?.id ?: "",
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            isActive = statusIsActive,
                            department = department,
                            position = position,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colors.border)

                // Role Management
                Text(
                    text = "Assign Role",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.text1,
                )
                Text(
                    "Note: Selecting a role will replace the current assignment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))

                roles.forEach { role ->
                    val isAssigned = selectedUser?.roles?.contains(role.name) == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isAssigned,
                            onClick = {
                                if (!isAssigned) {
                                    viewModel.assignRole(selectedUser?.id ?: "", role.name)
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.textSecondary,
                            ),
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(role.name, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            role.description?.let { desc ->
                                if (desc.isNotEmpty()) {
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(32.dp))

                // Actions
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.cancelled.copy(alpha = 0.1f),
                        contentColor = colors.cancelled,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null,
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete User", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm && selectedUser != null) {
        ConfirmDialog(
            title = "Delete User",
            message = "Are you sure you want to delete user '${selectedUser.username}'? This action cannot be undone.",
            confirmText = "Delete User",
            destructive = true,
            onConfirm = {
                viewModel.deleteUser(selectedUser.id ?: "")
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun UserDetailItem(label: String, value: String) {
    val colors = fleetColors
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}
