package org.solodev.fleet.mngt.features.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.ServerErrorDialog
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsersListScreen(router: AppRouter) {
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val userRoles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val colors = fleetColors

    // ADMIN-only guard
    LaunchedEffect(userRoles) {
        if (authStatus !is AuthStatus.Loading && !userRoles.contains(UserRole.ADMIN)) {
            router.clearAndNavigate(Screen.Dashboard)
        }
    }

    if (!userRoles.contains(UserRole.ADMIN)) return

    val vm = koinViewModel<UsersViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val selectedUser by vm.selectedUser.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state) { if (state is UiState.Error) showErrorDialog = true }

    if (showErrorDialog && state is UiState.Error) {
        ServerErrorDialog(
            message = (state as UiState.Error).message,
            onRetry = {
                vm.refresh()
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false },
        )
    }

    if (showAddSheet) {
        AddUserBottomSheet(onDismiss = { showAddSheet = false }, viewModel = vm)
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main Content Area
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                // Header Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "User Management",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Manage system users, roles, and access permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = vm::onSearchQueryChange,
                            placeholder = { Text("Search users...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null, tint = colors.textSecondary)
                            },
                            modifier = Modifier.width(300.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        Button(
                            onClick = { showAddSheet = true },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Register User", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Table Area
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                ) {
                    when (val s = state) {
                        is UiState.Loading -> TableSkeleton(rows = 10, columnCount = 5)
                        is UiState.Error -> TableSkeleton(rows = 10, columnCount = 5)
                        is UiState.Success ->
                            PaginatedTable(
                                headers =
                                listOf(
                                    "USER",
                                    "EMAIL",
                                    "ROLES",
                                    "STATUS",
                                    "VERIFIED",
                                ),
                                items = s.data,
                                onRowClick = { idx -> vm.selectUser(s.data[idx]) },
                                emptyMessage = "No users found matching your search.",
                                rowContent = { user, _ ->
                                    // Name & Profile
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(0.8f).padding(vertical = 8.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text =
                                                    user.firstName
                                                        ?.take(1)
                                                        ?.uppercase()
                                                        ?: "?",
                                                    color = colors.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp),
                                        ) {
                                            Text(
                                                text = user.fullName
                                                    ?: "${user.firstName} ${user.lastName}",
                                                style =
                                                MaterialTheme.typography
                                                    .bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary,
                                            )
                                            Text(
                                                text = "@${user.username ?: "user"}",
                                                style =
                                                MaterialTheme.typography
                                                    .labelSmall,
                                                color = colors.textSecondary,
                                            )
                                        }
                                    }

                                    // Email
                                    Text(
                                        text = user.email ?: "—",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center,
                                    )

                                    // Roles
                                    FlowRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        user.roles?.forEach { role ->
                                            Surface(
                                                color =
                                                colors.primary.copy(
                                                    alpha = 0.05f,
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                            ) {
                                                Text(
                                                    text = role,
                                                    modifier =
                                                    Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 2.dp,
                                                    ),
                                                    style =
                                                    MaterialTheme.typography
                                                        .labelSmall,
                                                    color = colors.primary,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                            ?: Text("—", color = colors.textSecondary)
                                    }

                                    // Active Status
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val isActive = user.isActive == true
                                        Surface(
                                            color =
                                            (
                                                if (isActive) {
                                                    colors.success
                                                } else {
                                                    colors.cancelled
                                                }
                                                )
                                                .copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Row(
                                                modifier =
                                                Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp,
                                                ),
                                                verticalAlignment =
                                                Alignment.CenterVertically,
                                                horizontalArrangement =
                                                Arrangement.Center,
                                            ) {
                                                Box(
                                                    modifier =
                                                    Modifier.size(6.dp)
                                                        .background(
                                                            if (isActive) {
                                                                colors.success
                                                            } else {
                                                                colors.cancelled
                                                            },
                                                            RoundedCornerShape(
                                                                3.dp,
                                                            ),
                                                        ),
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text =
                                                    if (isActive) {
                                                        "Active"
                                                    } else {
                                                        "Inactive"
                                                    },
                                                    style =
                                                    MaterialTheme.typography
                                                        .labelSmall,
                                                    color =
                                                    if (isActive) {
                                                        colors.success
                                                    } else {
                                                        colors.cancelled
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }

                                    // Verified Status
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val isVerified = user.isVerified == true
                                        Text(
                                            text =
                                            if (isVerified) {
                                                "Verified"
                                            } else {
                                                "Pending"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color =
                                            if (isVerified) {
                                                colors.success
                                            } else {
                                                colors.warning
                                            },
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                },
                            )
                    }
                }
            }

            // Side Panel
            UserDetailPanel(
                selectedUser = selectedUser,
                onClose = { vm.selectUser(null) },
                viewModel = vm,
            )
        }
    }
}
