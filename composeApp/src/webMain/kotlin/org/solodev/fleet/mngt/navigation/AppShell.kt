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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val navItems = listOf(
    NavItem("Dashboard",    Icons.Filled.Dashboard,       Screen.Dashboard),
    NavItem("Vehicles",     Icons.Filled.DirectionsCar,   Screen.Vehicles),
    NavItem("Rentals",      Icons.AutoMirrored.Filled.ReceiptLong, Screen.Rentals),
    NavItem("Customers",    Icons.Filled.Group,           Screen.Customers),
    NavItem("Maintenance",  Icons.Filled.Build,           Screen.Maintenance),
    NavItem("Accounting",   Icons.Filled.AccountBalance,  Screen.Accounting),
    NavItem("Live Tracking",Icons.Filled.LocationOn,      Screen.LiveTracking),
    NavItem("Reports",      Icons.Filled.Description,     Screen.Reports),
)

private val bottomNavItems = listOf(
    NavItem("Settings",     Icons.Filled.Settings,        Screen.Settings),
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
                .width(220.dp)
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
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Text(
                    "Fleet Manager",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = colors.border,
            )
            Spacer(Modifier.height(6.dp))

            navItems.forEach { item ->
                SidebarItem(
                    item = item,
                    isSelected = router.currentScreen::class == item.screen::class,
                    onClick = { router.navigate(item.screen) },
                )
            }

            if (isAdmin) {
                SidebarItem(
                    item = NavItem("Users", Icons.Filled.AdminPanelSettings, Screen.Users),
                    isSelected = router.currentScreen is Screen.Users,
                    onClick = { router.navigate(Screen.Users) },
                )
            }

            // Push Settings to the bottom of the sidebar
            Spacer(Modifier.weight(1f))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = colors.border,
            )
            Spacer(Modifier.height(4.dp))
            bottomNavItems.forEach { item ->
                SidebarItem(
                    item = item,
                    isSelected = router.currentScreen::class == item.screen::class,
                    onClick = { router.navigate(item.screen) },
                )
            }
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
            TopBar(router = router)
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
                    if (isSelected) colors.primary.copy(alpha = 0.1f)
                    else androidx.compose.ui.graphics.Color.Transparent
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
private fun TopBar(router: AppRouter) {
    val colors = fleetColors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.surface)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = screenTitle(router.currentScreen),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface.copy(alpha = 0.45f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
    }
}

private fun screenTitle(screen: Screen): String = when (screen) {
    is Screen.Dashboard      -> "Dashboard"
    is Screen.Vehicles,
    is Screen.VehicleDetail  -> "Vehicles"
    is Screen.Rentals,
    is Screen.RentalDetail   -> "Rentals"
    is Screen.Customers,
    is Screen.CustomerDetail -> "Customers"
    is Screen.Maintenance,
    is Screen.MaintenanceDetail -> "Maintenance"
    is Screen.Accounting     -> "Accounting"
    is Screen.LiveTracking   -> "Live Tracking"
    is Screen.Reports        -> "Reports"
    is Screen.Settings       -> "Settings"
    else                     -> "Fleet Manager"
}
