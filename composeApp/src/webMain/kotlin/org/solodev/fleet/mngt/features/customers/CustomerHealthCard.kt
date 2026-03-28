package org.solodev.fleet.mngt.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.ExperimentalTime
import org.solodev.fleet.mngt.api.dto.customer.CustomerDto
import org.solodev.fleet.mngt.theme.fleetColors
import kotlin.time.Clock

@OptIn(ExperimentalTime::class)
@Composable
fun CustomerHealthCard(customer: CustomerDto) {
    val colors = fleetColors
    val expiryMs = customer.licenseExpiryMs ?: 0L
    
    val nowMs = try {
        Clock.System.now().toEpochMilliseconds()
    } catch (e: Exception) {
        0L
    }
    
    val daysRemaining = if (expiryMs > nowMs) (expiryMs - nowMs) / (1000L * 60 * 60 * 24) else 0L
    val progress = if (daysRemaining >= 30) 1.0f else (daysRemaining.toFloat() / 30f).coerceIn(0f, 1f)
    
    val statusColor = when {
        daysRemaining >= 30 -> colors.active
        daysRemaining > 7 -> colors.maintenance
        else -> colors.cancelled
    }

    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Driver License Health", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.onBackground)
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            daysRemaining >= 30 -> "HEALTHY"
                            daysRemaining > 0 -> "EXPIRING SOON"
                            else -> "EXPIRED"
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$daysRemaining days remaining", fontSize = 12.sp, color = colors.onBackground.copy(alpha = 0.7f))
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = colors.border.copy(alpha = 0.3f)
                )
            }
        }
    }
}
