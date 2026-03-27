package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.theme.FleetSpacing
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun <T> PaginatedTable(
    headers:       List<String>,
    items:         List<T>,
    rowContent:    @Composable RowScope.(item: T, index: Int) -> Unit,
    modifier:      Modifier = Modifier,
    onRowClick:    ((index: Int) -> Unit)? = null,
    isLoading:     Boolean = false,
    emptyMessage:  String = "No records found.",
    emptyContent:  (@Composable () -> Unit)? = null,
    filterSlot:    (@Composable () -> Unit)? = null,
    pageSize:      Int = 20,
) {
    val colors = fleetColors

    var currentPage by remember { mutableStateOf(0) }
    LaunchedEffect(items.size) { currentPage = 0 }

    val totalPages = maxOf(1, (items.size + pageSize - 1) / pageSize)
    val pageItems  = items.drop(currentPage * pageSize).take(pageSize)
    val fromIdx    = if (items.isEmpty()) 0 else currentPage * pageSize + 1
    val toIdx      = minOf(currentPage * pageSize + pageItems.size, items.size)

    Surface(
        color    = colors.background,
        border   = BorderStroke(1.dp, colors.border),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // Filter slot
            filterSlot?.let { slot ->
                Box(Modifier.padding(horizontal = FleetSpacing.md, vertical = 12.dp)) { slot() }
                HorizontalDivider(color = colors.border)
            }

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = FleetSpacing.md, vertical = 11.dp),
            ) {
                headers.forEach { header ->
                    Text(
                        text       = header,
                        modifier   = Modifier.weight(1f),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.text2,
                    )
                }
            }
            HorizontalDivider(color = colors.border)

            // Body
            when {
                isLoading -> TableSkeleton(rows = 5, columnCount = headers.size)

                items.isEmpty() -> {
                    if (emptyContent != null) {
                        emptyContent()
                    } else {
                        EmptyState(
                            title = "No results found",
                            description = emptyMessage
                        )
                    }
                }

                else -> pageItems.forEachIndexed { pageIdx, item ->
                    val globalIdx = currentPage * pageSize + pageIdx
                    var hovered by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FleetSpacing.tableRowHeight)
                            .background(if (hovered && onRowClick != null) colors.surface else Color.Transparent)
                            .then(
                                if (onRowClick != null)
                                    Modifier.clickable { onRowClick(globalIdx) }
                                else Modifier
                            )
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> hovered = true
                                            PointerEventType.Exit  -> hovered = false
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = FleetSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowContent(item, globalIdx)
                    }
                    if (pageIdx < pageItems.lastIndex) {
                        HorizontalDivider(color = colors.border)
                    }
                }
            }

            // Footer
            HorizontalDivider(color = colors.border)
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FleetSpacing.md, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text     = if (items.isEmpty()) "No results" else "Showing $fromIdx–$toIdx of ${items.size} results",
                    color    = colors.text2,
                    fontSize = 13.sp,
                )
                if (totalPages > 1) {
                    PaginationBar(
                        currentPage  = currentPage,
                        totalPages   = totalPages,
                        onPageChange = { currentPage = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage:  Int,
    totalPages:   Int,
    onPageChange: (Int) -> Unit,
) {
    val colors = fleetColors
    val pages  = buildPageList(currentPage, totalPages)

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick  = { onPageChange(currentPage - 1) },
            enabled  = currentPage > 0,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous page",
                tint               = if (currentPage > 0) colors.text1 else colors.text2,
                modifier           = Modifier.size(16.dp),
            )
        }

        pages.forEach { page ->
            if (page == null) {
                Text(
                    text     = "…",
                    color    = colors.text2,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            } else {
                val isSelected = page == currentPage
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else Color.Transparent)
                        .clickable { onPageChange(page) },
                ) {
                    Text(
                        text       = "${page + 1}",
                        color      = if (isSelected) colors.onPrimary else colors.text2,
                        fontSize   = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        IconButton(
            onClick  = { onPageChange(currentPage + 1) },
            enabled  = currentPage < totalPages - 1,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next page",
                tint               = if (currentPage < totalPages - 1) colors.text1 else colors.text2,
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

private fun buildPageList(current: Int, total: Int): List<Int?> {
    if (total <= 7) return (0 until total).map { it }
    val always = buildSet {
        add(0)
        add(total - 1)
        for (i in (current - 1)..(current + 1)) if (i in 0 until total) add(i)
    }.sorted()
    val result = mutableListOf<Int?>()
    var prev = -1
    for (p in always) {
        if (p - prev > 1) result.add(null)
        result.add(p)
        prev = p
    }
    return result
}
