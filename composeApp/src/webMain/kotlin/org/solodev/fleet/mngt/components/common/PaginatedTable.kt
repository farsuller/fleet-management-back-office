package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.solodev.fleet.mngt.theme.FleetColors
import org.solodev.fleet.mngt.theme.FleetSpacing

/**
 * Generic paginated data table.
 *
 * @param headers     Column header labels
 * @param rows        List of rows; each row is a list of cell strings matching header count
 * @param onRowClick  Invoked with the row index when a row is clicked
 * @param hasMore     Whether a "Load More" button should be shown
 * @param isLoadingMore Whether the load-more operation is in progress
 * @param onLoadMore  Callback for the "Load More" button
 * @param isLoading   When true renders skeleton rows instead of real data
 * @param filterSlot  Optional composable rendered above the table as a filter bar
 */
@Composable
fun <T> PaginatedTable(
    headers:       List<String>,
    items:         List<T>,
    rowContent:    @Composable (item: T, index: Int) -> Unit,
    modifier:      Modifier = Modifier,
    onRowClick:    ((index: Int) -> Unit)? = null,
    hasMore:       Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore:    () -> Unit = {},
    isLoading:     Boolean = false,
    emptyMessage:  String = "No records found.",
    filterSlot:    (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Filter bar
        filterSlot?.invoke()

        if (filterSlot != null) {
            Spacer(Modifier.height(FleetSpacing.sm))
        }

        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = FleetSpacing.md, vertical = 12.dp),
        ) {
            headers.forEach { header ->
                Text(
                    text       = header.uppercase(),
                    modifier   = Modifier.weight(1f),
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = FleetColors.Border)

        when {
            isLoading -> {
                TableSkeleton(rows = 5, columnCount = headers.size)
            }
            items.isEmpty() -> {
                Box(
                    modifier         = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FleetSpacing.tableRowHeight)
                            .then(
                                if (onRowClick != null)
                                    Modifier.clickable { onRowClick(index) }
                                else Modifier
                            )
                            .padding(horizontal = FleetSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowContent(item, index)
                    }
                    HorizontalDivider(color = FleetColors.Border)
                }
            }
        }

        // Load More
        if (hasMore) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(FleetSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick  = onLoadMore,
                    enabled  = !isLoadingMore,
                ) {
                    Text(if (isLoadingMore) "Loading…" else "Load More")
                }
            }
        }
    }
}
