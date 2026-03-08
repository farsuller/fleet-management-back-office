package org.solodev.fleet.mngt.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.FleetSpacing
import org.solodev.fleet.mngt.theme.ThemeState
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.theme.fleetThemeState

@Composable
fun SettingsScreen() {
    val colors = fleetColors
    val themeState = fleetThemeState

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )

        // ── Appearance section ────────────────────────────────────────────────
        SettingsSection(title = "Appearance") {
            ThemeToggleRow(themeState = themeState)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = fleetColors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text2,
            letterSpacing = 0.8.sp,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(FleetSpacing.cardRadius),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ThemeToggleRow(themeState: ThemeState) {
    val colors = fleetColors
    val isDark = themeState.isDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Mode icon chip
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isDark) "Dark Mode" else "Light Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text1,
                )
                Text(
                    text = if (isDark) "Interface uses dark surfaces" else "Interface uses light surfaces",
                    fontSize = 12.sp,
                    color = colors.text2,
                )
            }
        }

        Switch(
            checked = isDark,
            onCheckedChange = { themeState.isDark = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor      = Color.White,
                checkedTrackColor      = colors.primary,
                uncheckedThumbColor    = Color.White,
                uncheckedTrackColor    = colors.border,
                uncheckedBorderColor   = colors.border,
            ),
        )
    }
}
