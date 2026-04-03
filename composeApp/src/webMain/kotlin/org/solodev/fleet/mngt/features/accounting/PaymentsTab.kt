package org.solodev.fleet.mngt.features.accounting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun PaymentsTab() {
    val vm = koinViewModel<PaymentsViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val colors = fleetColors

    var methodFilter by remember { mutableStateOf("") }
    var fromDateText by remember { mutableStateOf("") }
    var toDateText by remember { mutableStateOf("") }

    val fromMs = remember(fromDateText) {
        runCatching { LocalDate.parse(fromDateText.trim()).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull()
    }
    val toMs = remember(toDateText) {
        runCatching { LocalDate.parse(toDateText.trim()).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            IconButton(onClick = vm::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = fromDateText,
                onValueChange = { fromDateText = it },
                label = { Text("From (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = toDateText,
                onValueChange = { toDateText = it },
                label = { Text("To (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = methodFilter,
                onValueChange = { methodFilter = it },
                label = { Text("Payment Method") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        when (val s = state) {
            is UiState.Loading -> TableSkeleton(rows = 8)
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            is UiState.Success -> {
                val filtered = s.data.filter { payment ->
                    (methodFilter.isBlank() || (payment.paymentMethod ?: "").contains(methodFilter, ignoreCase = true)) &&
                        (fromMs == null || (payment.paymentDate ?: 0L) >= fromMs) &&
                        (toMs == null || (payment.paymentDate ?: 0L) <= toMs)
                }
                PaginatedTable(
                    headers = listOf("Payment #", "Invoice #", "Amount ()", "Method", "Type", "Date"),
                    items = filtered,
                    onRowClick = {},
                    emptyMessage = "No payments found",
                    rowContent = { payment, _ ->
                        Text(
                            payment.paymentNumber ?: payment.id?.take(8)?.let { "$it..." } ?: "--",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.text1,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            payment.invoiceId?.take(8)?.let { "$it..." } ?: "--",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.text1,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            formatPhp(payment.amount ?: 0L),
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.text1,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            payment.paymentMethod ?: "--",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.text2,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            payment.collectionType?.name?.replace('_', ' ') ?: "--",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.text2,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            payment.paymentDate?.let { formatDate(it) } ?: "--",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.text2,
                            textAlign = TextAlign.Center,
                        )
                    },
                )
            }
        }
    }
}
