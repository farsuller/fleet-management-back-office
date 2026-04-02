package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.solodev.fleet.mngt.theme.fleetColors

@Composable
fun PhoneNumberOutlinedTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val colors = fleetColors

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Strip any accidental non-digits (like if they paste a number)
            val digitsOnly = input.filter { it.isDigit() }

            if (digitsOnly.length <= 10) {
                onValueChange(digitsOnly)
            }
        },
        label = { Text(label) },
        modifier = modifier,
        prefix = {
            Text("+63 ", color = colors.onBackground.copy(alpha = 0.6f))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = { Text("9xx xxx xxxx") },
        singleLine = true,
    )
}
