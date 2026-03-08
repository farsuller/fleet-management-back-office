package org.solodev.fleet.mngt.features.accounting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

private fun formatDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

/** Formats centavo amount to ₱X,XXX.XX without String.format (unavailable in Kotlin/Wasm). */
private fun formatPhp(centavos: Long): String {
    val negative = centavos < 0
    val abs = if (negative) -centavos else centavos
    val pesos = abs / 100
    val cents = abs % 100
    val centsStr = cents.toString().padStart(2, '0')
    val pesosStr = pesos.toString()
    val withCommas = buildString {
        pesosStr.forEachIndexed { i, c ->
            val remaining = pesosStr.length - i
            if (i > 0 && remaining % 3 == 0) append(',')
            append(c)
        }
    }
    return "${if (negative) "-" else ""}\u20b1 $withCommas.$centsStr"
}

private fun InvoiceStatus?.badgeColor(): Color = when (this) {
    InvoiceStatus.PAID      -> Color(0xFF16A34A)
    InvoiceStatus.OVERDUE   -> Color(0xFFEF4444)
    InvoiceStatus.PENDING   -> Color(0xFFF59E0B)
    InvoiceStatus.DRAFT     -> Color(0xFF6B7280)
    InvoiceStatus.CANCELLED -> Color(0xFF9CA3AF)
    else                    -> Color(0xFF9CA3AF)
}

@Composable
fun InvoicesTab(router: AppRouter) {
    val vm = koinViewModel<InvoicesViewModel>()
    val state by vm.listState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val createResult by vm.createResult.collectAsState()
    val colors = fleetColors
    var detailInvoice by remember { mutableStateOf<InvoiceDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    val nowMs = Clock.System.now().toEpochMilliseconds()

    LaunchedEffect(createResult) {
        createResult?.onSuccess { showCreateDialog = false; createError = null }
            ?.onFailure { createError = it.message }
        if (createResult != null) vm.clearCreateResult()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRefreshing) CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = colors.primary)
                }
                Button(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Create Invoice")
                }
            }
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
            is UiState.Success -> PaginatedTable(
                headers = listOf("Invoice #", "Customer", "Rental #", "Status", "Amount (₱)", "Due Date"),
                items = s.data,
                onRowClick = { idx -> detailInvoice = s.data[idx] },
                emptyMessage = "No invoices found",
                rowContent = { invoice, _ ->
                    val isOverdue = invoice.status != InvoiceStatus.PAID &&
                        (invoice.dueDateMs ?: 0L) in 1 until nowMs
                    Text(
                        (invoice.id ?: "").take(8) + "…",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = colors.text1,
                    )
                    Text(invoice.customerName ?: "—", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.text1)
                    Text((invoice.rentalId ?: "").take(8) + "…", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.text2)
                    Text(
                        invoice.status?.name ?: "—",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = invoice.status.badgeColor(),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        formatPhp(invoice.amountPhp ?: 0L),
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = colors.text1,
                    )
                    Text(
                        invoice.dueDateMs?.let { formatDate(it) } ?: "—",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = if (isOverdue) Color(0xFFEF4444) else colors.text1,
                    )
                },
            )
        }
    }

    detailInvoice?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            paymentMethods = (vm.paymentMethods.collectAsState().value).filter { it.isActive == true },
            onPay = { methodId, amtPhp ->
                vm.payInvoice(invoice.id ?: "", methodId, amtPhp)
                detailInvoice = null
            },
            onDismiss = { detailInvoice = null },
        )
    }

    if (showCreateDialog) {
        CreateInvoiceDialog(
            error = createError,
            onSubmit = { rentalId, dueDateMs -> vm.createInvoice(rentalId, dueDateMs) },
            onDismiss = { showCreateDialog = false; createError = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDetailDialog(
    invoice: InvoiceDto,
    paymentMethods: List<PaymentMethodDto>,
    onPay: (methodId: String, amountPhp: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = fleetColors
    val canPay = invoice.status == InvoiceStatus.PENDING || invoice.status == InvoiceStatus.OVERDUE
    var selectedMethod by remember { mutableStateOf(paymentMethods.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Invoice Detail", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)

                DetailRow("Invoice #", invoice.id ?: "—")
                DetailRow("Customer", invoice.customerName ?: "—")
                DetailRow("Rental #", invoice.rentalId ?: "—")
                DetailRow("Amount", formatPhp(invoice.amountPhp ?: 0L))
                DetailRow("Status", invoice.status?.name ?: "—", invoice.status.badgeColor())
                invoice.dueDateMs?.let { DetailRow("Due Date", formatDate(it)) }
                invoice.paidAt?.let { DetailRow("Paid At", formatDate(it)) }

                if (canPay && paymentMethods.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Pay Invoice", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedMethod?.name ?: "Select payment method",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name ?: "") },
                                    onClick = { selectedMethod = method; expanded = false },
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                selectedMethod?.id?.let { methodId ->
                                    onPay(methodId, invoice.amountPhp ?: 0L)
                                }
                            },
                            enabled = selectedMethod != null,
                        ) { Text("Confirm Payment") }
                        Button(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors()) {
                            Text("Cancel")
                        }
                    }
                } else {
                    Button(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors()) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    val colors = fleetColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(100.dp), color = colors.text2, fontSize = 13.sp)
        Text(value, color = valueColor ?: colors.text1, fontSize = 13.sp)
    }
}

@Composable
private fun CreateInvoiceDialog(
    error: String?,
    onSubmit: (rentalId: String, dueDateMs: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var rentalId by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Create Invoice", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = rentalId,
                    onValueChange = { rentalId = it; validationError = null },
                    label = { Text("Rental ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it; validationError = null },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                (validationError ?: error)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val id = rentalId.trim()
                        if (id.isBlank()) { validationError = "Rental ID is required"; return@Button }
                        val dueDateMs = runCatching {
                            LocalDate.parse(dueDateText.trim()).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                        }.getOrElse { validationError = "Date must be YYYY-MM-DD"; return@Button }
                        validationError = null
                        onSubmit(id, dueDateMs)
                    }) { Text("Create") }
                }
            }
        }
    }
}
