package org.solodev.fleet.mngt.navigation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.solodev.fleet.mngt.auth.AppDependencyDispatcher
import org.solodev.fleet.mngt.auth.AuthStatus
import org.solodev.fleet.mngt.auth.UserRole
import org.solodev.fleet.mngt.theme.fleetColors

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
)

private data class NavSection(
    val title: String,
    val items: List<NavItem>,
)

private val navSections = listOf(
    NavSection(
        "MAIN",
        listOf(
            NavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard),
        ),
    ),
    NavSection(
        "FLEET MANAGEMENT",
        listOf(
            NavItem("Vehicles", Icons.Default.DirectionsCar, Screen.Vehicles),
            NavItem("Drivers", Icons.Default.Badge, Screen.Drivers),
            NavItem("Maintenance", Icons.Default.Build, Screen.Maintenance),
            NavItem("Live Tracking", Icons.Default.LocationOn, Screen.LiveTracking),
        ),
    ),
    NavSection(
        "BOOKINGS",
        listOf(
            NavItem("Rentals", Icons.AutoMirrored.Filled.ReceiptLong, Screen.Rentals),
        ),
    ),
    NavSection(
        "CUSTOMERS",
        listOf(
            NavItem("Customers", Icons.Default.Group, Screen.Customers),
        ),
    ),
    NavSection(
        "FINANCE & REPORTS",
        listOf(
            NavItem("Accounting", Icons.Default.AccountBalance, Screen.Accounting),
            NavItem("Reports", Icons.Default.Description, Screen.Reports),
        ),
    ),
    NavSection(
        "SETTINGS",
        listOf(
            NavItem("Settings", Icons.Default.Settings, Screen.Settings),
        ),
    ),
)

@Composable
fun AppShell(
    router: AppRouter,
    content: @Composable () -> Unit,
) {
    val colors = fleetColors
    val dispatcher = koinInject<AppDependencyDispatcher>()
    val authStatus by dispatcher.status.collectAsState()
    val isAdmin = (authStatus as? AuthStatus.Authenticated)
        ?.session?.roles?.contains(UserRole.ADMIN) == true

    Row(Modifier.fillMaxSize()) {
        // ── Sidebar ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(240.dp) // Slightly wider for the sections
                .fillMaxHeight()
                .background(colors.surface)
                .padding(vertical = 16.dp),
        ) {
            // Logo / brand
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    "Fleet Manager",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                navSections.forEach { section ->
                    SidebarCategoryHeader(section.title)
                    section.items.forEach { item ->
                        SidebarItem(
                            item = item,
                            isSelected = when (item.screen) {
                                is Screen.Dashboard -> router.currentScreen is Screen.Dashboard
                                is Screen.Vehicles -> router.currentScreen is Screen.Vehicles || router.currentScreen is Screen.VehicleDetail
                                is Screen.Rentals -> router.currentScreen is Screen.Rentals || router.currentScreen is Screen.RentalDetail
                                is Screen.Customers -> router.currentScreen is Screen.Customers || router.currentScreen is Screen.CustomerDetail
                                is Screen.Maintenance -> router.currentScreen is Screen.Maintenance || router.currentScreen is Screen.MaintenanceDetail
                                is Screen.Settings -> router.currentScreen is Screen.Settings
                                else -> router.currentScreen == item.screen
                            },
                            onClick = { router.navigate(item.screen) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (isAdmin) {
                    SidebarCategoryHeader("ADMIN")
                    SidebarItem(
                        item = NavItem("Users", Icons.Default.AdminPanelSettings, Screen.Users),
                        isSelected = router.currentScreen is Screen.Users,
                        onClick = { router.navigate(Screen.Users) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = colors.border,
            )
            Spacer(Modifier.height(8.dp))
            SidebarActionItem(
                label = "Logout",
                icon = Icons.AutoMirrored.Default.Logout,
                onClick = { dispatcher.signOut() },
            )
        }
        // Sidebar right border
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
        // ── Main content area ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(colors.background),
        ) {
            TopBar(router = router, authStatus = authStatus)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SidebarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = fleetColors
    val textColor = if (isSelected) colors.primary else colors.onBackground.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        colors.primary.copy(alpha = 0.1f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                )
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(17.dp),
                tint = textColor,
            )
            Text(
                text = item.label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(colors.primary),
            )
        }
    }
}

@Composable
private fun SidebarActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val tint = androidx.compose.ui.graphics.Color(0xFFEF4444)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(17.dp),
                tint = tint,
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = tint,
            )
        }
    }
}

@Composable
private fun TopBar(router: AppRouter, authStatus: AuthStatus) {
    val colors = fleetColors
    val userSession = (authStatus as? AuthStatus.Authenticated)?.session

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.surface)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = screenTitle(router.currentScreen),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface.copy(alpha = 0.45f),
            )

            userSession?.let { session ->
                UserAvatar(session)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
    }
}

@Composable
private fun UserAvatar(session: org.solodev.fleet.mngt.auth.UserSession) {
    val colors = fleetColors
    var isHovered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hoverJob by remember { mutableStateOf<Job?>(null) }
    val firstLetter = session.fullName.firstOrNull()?.toString()?.uppercase() ?: "?"

    // Larger hit area matching the header height (56dp)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                hoverJob?.cancel()
                                isHovered = true
                            }

                            PointerEventType.Exit -> {
                                hoverJob = scope.launch {
                                    delay(200)
                                    isHovered = false
                                }
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primary.copy(alpha = 0.1f))
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = firstLetter,
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (isHovered) {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = IntOffset(x = 0, y = 4),
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Enter -> {
                                            hoverJob?.cancel()
                                            isHovered = true
                                        }

                                        PointerEventType.Exit -> {
                                            hoverJob = scope.launch {
                                                delay(200)
                                                isHovered = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .padding(1.dp)
                        .background(colors.border.copy(alpha = 0.1f), RoundedCornerShape(11.dp))
                        .padding(1.dp)
                        .background(colors.surface, RoundedCornerShape(10.dp))
                        .padding(16.dp)
                        .width(220.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = firstLetter,
                                    color = colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = session.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = session.email,
                                    fontSize = 12.sp,
                                    color = colors.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = fleetColors.onBackground.copy(alpha = 0.4f),
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth(),
    )
}

private fun screenTitle(screen: Screen): String = when (screen) {
    is Screen.Dashboard -> "Dashboard"
    is Screen.Vehicles,
    is Screen.VehicleDetail,
    -> "Vehicles"

    is Screen.Drivers -> "Drivers"
    is Screen.Rentals,
    is Screen.RentalDetail,
    -> "Rentals"

    is Screen.Customers,
    is Screen.CustomerDetail,
    -> "Customers"

    is Screen.Maintenance,
    is Screen.MaintenanceDetail,
    -> "Maintenance"

    is Screen.Accounting -> "Accounting"
    is Screen.LiveTracking -> "Live Tracking"
    is Screen.Reports -> "Reports"
    is Screen.Users -> "Users"
    is Screen.Settings -> "Settings"
    else -> "Fleet Manager"
}
