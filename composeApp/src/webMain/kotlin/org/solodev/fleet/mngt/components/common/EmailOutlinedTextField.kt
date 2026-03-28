package org.solodev.fleet.mngt.components.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

@Composable
fun EmailOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

// Reactive error state
    val isEmailError by remember(value) {
        derivedStateOf {
            value.isNotEmpty() && !emailRegex.matches(value)
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email Address") },
        modifier = modifier,
        isError = isEmailError,
        supportingText = {
            if (isEmailError) {
                Text(
                    text = "Please enter a valid email (e.g., name@domain.com)",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Unspecified,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Unspecified,
            platformImeOptions = null,
            showKeyboardOnFocus = null,
            hintLocales = null
        ),
        singleLine = true,
        placeholder = { Text("example@mail.com") }
    )

}