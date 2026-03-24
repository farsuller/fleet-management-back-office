package org.solodev.fleet.mngt.features.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceJobDto
import org.solodev.fleet.mngt.api.dto.maintenance.MaintenanceStatus
import org.solodev.fleet.mngt.components.common.ConfirmDialog
import org.solodev.fleet.mngt.components.common.MaintenanceStatusBadge
import org.solodev.fleet.mngt.components.common.MaintenanceStatus as UiMaintenanceStatus
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun MaintenanceDetailScreen(jobId: String, router: AppRouter) {
    val vm = koinViewModel<MaintenanceViewModel>()
    val detailState by vm.detailState.collectAsState()
    val actionResult by vm.actionResult.collectAsState()
    val colors = fleetColors

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jobId) { vm.loadJob(jobId) }
    LaunchedEffect(actionResult) {
        actionResult?.onFailure { errorMessage = it.message }
            ?.onSuccess { vm.loadJob(jobId) }
        if (actionResult != null) vm.clearActionResult()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { router.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }
            Text("Maintenance Detail", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        when (val s = detailState) {
            null, is UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.loadJob(jobId) }) { Text("Retry") }
            }
            is UiState.Success -> MaintenanceDetailContent(
                job = s.data,
                onStart = { vm.startJob(jobId) },
                onCancel = { vm.cancelJob(jobId) },
                onComplete = { labor, parts -> vm.completeJob(jobId, labor, parts) },
            )
        }
    }
}

@Composable
private fun MaintenanceDetailContent(
    job: MaintenanceJobDto,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onComplete: (laborCostPhp: Long, partsCostPhp: Long) -> Unit,
) {
    val colors = fleetColors
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    fun statusToUi(s: MaintenanceStatus?) = when (s) {
        MaintenanceStatus.SCHEDULED   -> UiMaintenanceStatus.SCHEDULED
        MaintenanceStatus.IN_PROGRESS -> UiMaintenanceStatus.IN_PROGRESS
        MaintenanceStatus.COMPLETED   -> UiMaintenanceStatus.COMPLETED
        MaintenanceStatus.CANCELLED   -> UiMaintenanceStatus.CANCELLED
        else                          -> UiMaintenanceStatus.CANCELLED
    }

    // Job info card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Job Info", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            MaintenanceStatusBadge(statusToUi(job.status))
        }
        DetailRow("Job ID", job.id ?: "-")
        DetailRow("Vehicle", job.vehiclePlate ?: "-")
        DetailRow("Type", job.type?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-")
        DetailRow("Priority", job.priority?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-")
        DetailRow("Scheduled", job.scheduledDate?.let { formatMaintenanceDate(it) } ?: "-")
        DetailRow("Est. Cost", job.estimatedCostPhp?.let { "PHP ${it / 100}" } ?: "-")
        job.description?.takeIf { it.isNotBlank() }?.let { DetailRow("Description", it) }
        job.createdAt?.let { DetailRow("Created", formatMaintenanceDate(it)) }
    }

    // Cost summary card (only when completed)
    if (job.status == MaintenanceStatus.COMPLETED) {
        val labor = job.laborCostPhp ?: 0L
        val parts = job.partsCostPhp ?: 0L
        val total = labor + parts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Cost Summary", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            DetailRow("Labor", "₱${labor / 100}")
            DetailRow("Parts", "₱${parts / 100}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("₱${total / 100}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }

    // Action buttons
    when (job.status) {
        MaintenanceStatus.SCHEDULED -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStart) { Text("Start Job") }
            OutlinedButton(
                onClick = { showCancelConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Cancel Job") }
        }
        MaintenanceStatus.IN_PROGRESS -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showCompleteDialog = true }) { Text("Complete Job") }
        }
        else -> Unit
    }

    // Dialogs
    if (showCancelConfirm) {
        ConfirmDialog(
            title = "Cancel Job",
            message = "Are you sure you want to cancel this maintenance job? This action cannot be undone.",
            confirmText = "Yes, Cancel",
            onConfirm = { showCancelConfirm = false; onCancel() },
            onDismiss = { showCancelConfirm = false },
        )
    }

    if (showCompleteDialog) {
        CompleteJobDialog(
            onConfirm = { labor, parts -> showCompleteDialog = false; onComplete(labor, parts) },
            onDismiss = { showCompleteDialog = false },
        )
    }
}

@Composable
private fun CompleteJobDialog(
    onConfirm: (laborCostPhp: Long, partsCostPhp: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var laborText by remember { mutableStateOf("") }
    var partsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Job") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the actual costs to mark this job as completed.", fontSize = 14.sp)
                OutlinedTextField(
                    value = laborText,
                    onValueChange = { laborText = it; error = null },
                    label = { Text("Labor Cost (PHP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = partsText,
                    onValueChange = { partsText = it; error = null },
                    label = { Text("Parts Cost (PHP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val labor = laborText.trim().toLongOrNull()
                val parts = partsText.trim().toLongOrNull()
                if (labor == null || labor < 0) { error = "Enter a valid labor cost"; return@Button }
                if (parts == null || parts < 0) { error = "Enter a valid parts cost"; return@Button }
                onConfirm(labor * 100L, parts * 100L) // convert to centavos
            }) { Text("Complete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = fleetColors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
