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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.CreateInvoiceRequest
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceDto
import org.solodev.fleet.mngt.api.dto.accounting.InvoiceStatus
import org.solodev.fleet.mngt.api.dto.accounting.PaymentMethodDto
import org.solodev.fleet.mngt.components.common.PaginatedTable
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState
import kotlin.time.Instant

internal fun formatDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    return "${dt.year}-${(dt.month.ordinal + 1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}

/** Formats whole-unit PHP amount (e.g. 2800 â†’ â‚± 2,800). Backend uses whole integers not centavos. */
internal fun formatPhp(amount: Long): String {
    val negative = amount < 0
    val abs = if (negative) -amount else amount
    val str = abs.toString()
    val withCommas = buildString {
        str.forEachIndexed { i, c ->
            val remaining = str.length - i
            if (i > 0 && remaining % 3 == 0) append(',')
            append(c)
        }
    }
    return "${if (negative) "-" else ""}\u20b1 $withCommas"
}

private fun InvoiceStatus?.badgeColor(): Color = when (this) {
    InvoiceStatus.PAID -> Color(0xFF16A34A)
    InvoiceStatus.OVERDUE -> Color(0xFFEF4444)
    InvoiceStatus.PENDING,
    InvoiceStatus.ISSUED,
    -> Color(0xFFF59E0B)

    InvoiceStatus.DRAFT -> Color(0xFF6B7280)
    InvoiceStatus.CANCELLED -> Color(0xFF9CA3AF)
    else -> Color(0xFF9CA3AF)
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

    LaunchedEffect(createResult) {
        createResult?.onSuccess {
            showCreateDialog = false
            createError = null
        }
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
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                headers = listOf("Invoice #", "Customer", "Status", "Total", "Balance", "Due Date"),
                items = s.data,
                onRowClick = { idx -> detailInvoice = s.data[idx] },
                emptyMessage = "No invoices found",
                rowContent = { invoice, _ ->
                    InvoiceText(
                        modifier = Modifier.weight(1f),
                        text = invoice.invoiceNumber ?: ((invoice.id ?: "").take(8) + "â€¦"),
                        color = colors.text1,
                    )
                    InvoiceText(
                        modifier = Modifier.weight(1f),
                        text = invoice.customer?.fullName ?: "â€”",
                        color = colors.text1,
                    )
                    InvoiceText(
                        modifier = Modifier.weight(1f),
                        text = invoice.status?.name ?: "â€”",
                        color = invoice.status.badgeColor(),
                    )
                    InvoiceText(
                        modifier = Modifier.weight(1f),
                        text = formatPhp(invoice.total ?: 0L),
                        color = colors.text1,
                    )
                    InvoiceText(
                        modifier = Modifier.weight(1f),
                        text = formatPhp(invoice.balance ?: 0L),
                        color = if ((invoice.balance ?: 0L) > 0L && invoice.status != InvoiceStatus.PAID) {
                            Color(
                                0xFFEF4444,
                            )
                        } else {
                            colors.text1
                        },
                    )
                    InvoiceText(
                        text = invoice.dueDate?.let { formatDate(it) } ?: "â€”",
                        modifier = Modifier.weight(1f),
                        color = colors.text1,
                    )
                },
            )
        }
    }

    detailInvoice?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            paymentMethods = vm.paymentMethods.collectAsState().value.filter { it.isActive == true },
            onPay = { method, amount ->
                vm.payInvoice(invoice.id ?: "", method, amount)
                detailInvoice = null
            },
            onDismiss = { detailInvoice = null },
        )
    }

    if (showCreateDialog) {
        CreateInvoiceDialog(
            error = createError,
            onSubmit = { req -> vm.createInvoice(req) },
            onDismiss = {
                showCreateDialog = false
                createError = null
            },
        )
    }
}

@Composable
fun InvoiceText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 13.sp,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDetailDialog(
    invoice: InvoiceDto,
    paymentMethods: List<PaymentMethodDto>,
    onPay: (method: String, amount: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = fleetColors
    val canPay = invoice.status == InvoiceStatus.PENDING ||
        invoice.status == InvoiceStatus.ISSUED ||
        invoice.status == InvoiceStatus.OVERDUE
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

                DetailRow("Invoice #", invoice.invoiceNumber ?: invoice.id ?: "â€”")
                invoice.customer?.let { c ->
                    DetailRow("Customer", c.fullName ?: "â€”")
                    c.email?.let { DetailRow("Email", it) }
                    c.phoneNumber?.let { DetailRow("Phone", it) }
                }
                invoice.rentalId?.let { DetailRow("Rental #", it.take(8) + "â€¦") }
                invoice.subtotal?.let { DetailRow("Subtotal", formatPhp(it)) }
                invoice.tax?.let { DetailRow("Tax", formatPhp(it)) }
                DetailRow("Total", formatPhp(invoice.total ?: 0L))
                DetailRow("Paid", formatPhp(invoice.paidAmount ?: 0L))
                DetailRow(
                    "Balance",
                    formatPhp(invoice.balance ?: 0L),
                    if ((invoice.balance ?: 0L) > 0) Color(0xFFEF4444) else Color(0xFF16A34A),
                )
                DetailRow("Status", invoice.status?.name ?: "â€”", invoice.status.badgeColor())
                invoice.dueDate?.let { DetailRow("Due Date", formatDate(it)) }
                invoice.paidDate?.let { DetailRow("Paid At", formatDate(it)) }

                if (canPay && paymentMethods.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Record Payment",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                    )

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedMethod?.name ?: "Select payment method",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name ?: "") },
                                    onClick = {
                                        selectedMethod = method
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                selectedMethod?.name?.let { method ->
                                    onPay(method, invoice.balance ?: invoice.total ?: 0L)
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
internal fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    val colors = fleetColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(100.dp), color = colors.text2, fontSize = 13.sp)
        Text(value, color = valueColor ?: colors.text1, fontSize = 13.sp)
    }
}

@Composable
private fun CreateInvoiceDialog(
    error: String?,
    onSubmit: (CreateInvoiceRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var customerId by remember { mutableStateOf("") }
    var rentalId by remember { mutableStateOf("") }
    var subtotalText by remember { mutableStateOf("") }
    var taxText by remember { mutableStateOf("0") }
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
                    value = customerId,
                    onValueChange = {
                        customerId = it
                        validationError = null
                    },
                    label = { Text("Customer ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = rentalId,
                    onValueChange = {
                        rentalId = it
                        validationError = null
                    },
                    label = { Text("Rental ID (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = subtotalText,
                    onValueChange = {
                        subtotalText = it
                        validationError = null
                    },
                    label = { Text("Subtotal (â‚±)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = taxText,
                    onValueChange = {
                        taxText = it
                        validationError = null
                    },
                    label = { Text("Tax (â‚±)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = {
                        dueDateText = it
                        validationError = null
                    },
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
                        val cId = customerId.trim()
                        if (cId.isBlank()) {
                            validationError = "Customer ID is required"
                            return@Button
                        }
                        val subtotal = subtotalText.trim().toLongOrNull()
                        if (subtotal == null || subtotal < 0) {
                            validationError = "Enter a valid subtotal"
                            return@Button
                        }
                        val tax = taxText.trim().toLongOrNull() ?: 0L
                        val dueDate = dueDateText.trim()
                        runCatching { LocalDate.parse(dueDate) }
                            .onFailure {
                                validationError = "Date must be YYYY-MM-DD"
                                return@Button
                            }
                        validationError = null
                        onSubmit(
                            CreateInvoiceRequest(
                                customerId = cId,
                                rentalId = rentalId.trim().ifBlank { null },
                                subtotal = subtotal,
                                tax = tax,
                                dueDate = "${dueDate}T00:00:00Z",
                            ),
                        )
                    }) { Text("Create") }
                }
            }
        }
    }
}
