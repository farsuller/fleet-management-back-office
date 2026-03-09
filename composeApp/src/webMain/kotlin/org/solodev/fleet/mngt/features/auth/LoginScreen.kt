package org.solodev.fleet.mngt.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.navigation.Screen
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

@Composable
fun LoginScreen(router: AppRouter) {
    val vm = koinViewModel<LoginViewModel>()
    val form by vm.form.collectAsState()
    val loginState by vm.loginState.collectAsState()
    val colors = fleetColors
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loginState) {
        when (loginState) {
            is UiState.Success -> router.replace(Screen.Dashboard)
            is UiState.Error  -> errorMessage = (loginState as UiState.Error).message
            else              -> {}
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Login Failed", color = colors.onSurface) },
            text  = { Text(errorMessage!!, color = colors.onSurface.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK", color = colors.primary)
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Fleet Manager",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Text(
                "Back Office Portal",
                fontSize = 14.sp,
                color = colors.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = form.email,
                onValueChange = vm::onEmailChange,
                label = { Text("Email") },
                isError = form.emailError != null,
                supportingText = form.emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.password,
                onValueChange = vm::onPasswordChange,
                label = { Text("Password") },
                isError = form.passwordError != null,
                supportingText = form.passwordError?.let { { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { vm.submit() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )


            Button(
                onClick = vm::submit,
                enabled = loginState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (loginState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary,
                    )
                } else {
                    Text("Sign In", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
