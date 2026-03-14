package org.solodev.fleet.mngt.features.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun UsersListScreen(router: AppRouter) {
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val roles = (authStatus as? AuthStatus.Authenticated)?.session?.roles ?: emptySet()
    val colors = fleetColors

    // ADMIN-only guard: redirect non-admins to dashboard
    LaunchedEffect(roles) {
        if (authStatus !is AuthStatus.Loading && !roles.contains(UserRole.ADMIN)) {
            router.clearAndNavigate(Screen.Dashboard)
        }
    }

    if (!roles.contains(UserRole.ADMIN)) return

    val vm = koinViewModel<UsersViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Users", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRefreshing) CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
            }
        }

        when (val s = state) {
            is UiState.Loading -> org.solodev.fleet.mngt.components.common.TableSkeleton(rows = 8)
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            is UiState.Success -> PaginatedTable(
                headers = listOf("Name", "Email", "Roles", "Verified", "Active"),
                items = s.data,
                onRowClick = { idx -> router.navigate(Screen.UserDetail(s.data[idx].id ?: "")) },
                emptyMessage = "No users found",
                rowContent = { user, _ ->
                    Text(
                        "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim(),
                        modifier = Modifier.weight(1.5f),
                        fontSize = 13.sp,
                        color = colors.text1,
                    )
                    Text(
                        user.email ?: "",
                        modifier = Modifier.weight(2f),
                        fontSize = 13.sp,
                        color = colors.text1,
                    )
                    Text(
                        user.roles?.joinToString(", ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } } ?: "—",
                        modifier = Modifier.weight(2f),
                        fontSize = 12.sp,
                        color = colors.text2,
                    )
                    Switch(
                        checked = user.isVerified == true,
                        onCheckedChange = null,
                        modifier = Modifier.weight(0.7f),
                    )
                    Switch(
                        checked = user.isActive == true,
                        onCheckedChange = null,
                        modifier = Modifier.weight(0.7f),
                    )
                },
            )
        }
    }
}
