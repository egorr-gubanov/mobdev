package lab3.egor.chat.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import lab3.egor.chat.R
import lab3.egor.chat.viewmodel.LoginUiState
import lab3.egor.chat.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedName by viewModel.savedName.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Подставляем имя, если оно сохранено
    LaunchedEffect(savedName) {
        if (savedName.isNotBlank() && name.isEmpty()) {
            name = savedName
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.login_username_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState is LoginUiState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (password.isEmpty()) {
                        viewModel.register(name)
                    } else {
                        viewModel.login(name, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = name.isNotEmpty()
            ) {
                Text(stringResource(R.string.login_button))
            }
        }

        if (uiState is LoginUiState.Error) {
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetState() }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                title = { Text(stringResource(R.string.error_auth)) }
            )
        }
    }
}
