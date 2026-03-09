package org.solodev.fleet.mngt.features.tracking.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog for importing a GeoJSON route obtained from https://geojson.io.
 *
 * Workflow:
 *  1. User draws a route on geojson.io and copies the GeoJSON.
 *  2. Opens this dialog, enters a route name, pastes the GeoJSON.
 *  3. Clicks "Import" — calls [onImport] with the name, optional description, and raw GeoJSON.
 */
@Composable
fun ImportRouteDialog(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onImport: (name: String, description: String?, geojson: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var geojson     by remember { mutableStateOf("") }

    val canImport = name.isNotBlank() && geojson.isNotBlank() && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text("Import Route from GeoJSON", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Draw a route at geojson.io, then copy the GeoJSON output and paste it below.",
                    fontSize = 12.sp,
                )

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Route Name *") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier      = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Description (optional)") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier      = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value         = geojson,
                    onValueChange = { geojson = it },
                    label         = { Text("GeoJSON *") },
                    placeholder   = { Text("Paste GeoJSON here…", fontSize = 11.sp) },
                    minLines      = 4,
                    maxLines      = 8,
                    modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                )

                if (errorMessage != null) {
                    Text(
                        text     = errorMessage,
                        fontSize = 12.sp,
                        color    = androidx.compose.ui.graphics.Color(0xFFEF4444),
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(
                    onClick  = { if (canImport) onImport(name.trim(), description.trimOrNull(), geojson.trim()) },
                    enabled  = canImport,
                ) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun String.trimOrNull(): String? = trim().ifBlank { null }
