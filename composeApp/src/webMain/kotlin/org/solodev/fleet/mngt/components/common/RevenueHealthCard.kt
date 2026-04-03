package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun RevenueHealthCard(
    revenuePhp: Long,
    modifier: Modifier = Modifier,
) {
    val colors = fleetColors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Currency Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.available.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "₱",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.available,
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Revenue (Completed)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2,
                    )
                    Text(
                        text = "₱${formatCurrency(revenuePhp)}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Subtitle / Trend mockup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.available.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "+12.5%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.available,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "vs yesterday",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text2,
                )
            }
        }
    }
}

private fun formatCurrency(amount: Long): String = amount.toString().reversed().chunked(3).joinToString(",").reversed()
