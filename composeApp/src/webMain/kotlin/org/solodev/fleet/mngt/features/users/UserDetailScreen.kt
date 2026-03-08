package org.solodev.fleet.mngt.features.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors

private val ALL_ROLES = listOf(
    "ADMIN", "FLEET_MANAGER", "CUSTOMER_SUPPORT", "RENTAL_AGENT", "DRIVER", "CUSTOMER",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserDetailScreen(userId: String, router: AppRouter) {
    val vm = koinViewModel<UsersViewModel>()
    val selectedUser by vm.selectedUser.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var pendingRoles by remember { mutableStateOf<List<String>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) { vm.loadUser(userId) }

    LaunchedEffect(actionResult) {
        actionResult?.onFailure { errorMsg = it.message }
        if (actionResult != null) vm.clearActionResult()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { router.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }
            Text("User Detail", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        }

        if (selectedUser == null) {
            CircularProgressIndicator()
            return@Column
        }

        val user = selectedUser!!
        val currentRoles = pendingRoles ?: (user.roles ?: emptyList())

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Profile card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailRow("Name", "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim())
            DetailRow("Email", user.email ?: "—")
            DetailRow("Verified", if (user.isVerified == true) "Yes" else "No")
            DetailRow("Active", if (user.isActive == true) "Yes" else "No")
        }

        // Role assignment
        Text("Roles", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_ROLES.forEach { role ->
                FilterChip(
                    selected = currentRoles.contains(role),
                    onClick = {
                        val updated = if (currentRoles.contains(role)) {
                            currentRoles - role
                        } else {
                            currentRoles + role
                        }
                        pendingRoles = updated
                    },
                    label = {
                        Text(
                            role.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                        )
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    vm.assignRoles(userId, currentRoles)
                    pendingRoles = null
                },
                enabled = pendingRoles != null,
            ) { Text("Save Roles") }
            if (pendingRoles != null) {
                Button(
                    onClick = { pendingRoles = null },
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) { Text("Reset") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Delete user
        Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Delete User") }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete User",
            message = "Are you sure you want to permanently delete this user? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                vm.deleteUser(userId) { router.clearAndNavigate(Screen.Users) }
            },
            onDismiss = { showDeleteConfirm = false },
            destructive = true,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = fleetColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(120.dp), color = colors.text2, fontSize = 13.sp)
        Text(value, color = colors.text1, fontSize = 13.sp)
    }
}
