package com.example.practicamoviles.presentation.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.practicamoviles.data.remote.FirebaseAuthManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    navController: NavController,
    registro: Boolean
) {
    var email by remember(registro) { mutableStateOf("") }
    var password by remember(registro) { mutableStateOf("") }
    var confirmacion by remember(registro) { mutableStateOf("") }
    var mensaje by remember(registro) { mutableStateOf("") }
    var procesando by remember(registro) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            androidx.compose.ui.Alignment.CenterVertically
        )
    ) {
        Text(
            text = "Lotería PC1",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = if (registro) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !procesando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !procesando,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (registro) {
            OutlinedTextField(
                value = confirmacion,
                onValueChange = { confirmacion = it },
                label = { Text("Confirmar contraseña") },
                singleLine = true,
                enabled = !procesando,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (mensaje.isNotBlank()) {
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val error = when {
                    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                        "Ingresa un correo válido"

                    password.isBlank() ->
                        "Ingresa tu contraseña"

                    registro && password.length < 6 ->
                        "La contraseña debe tener al menos seis caracteres"

                    registro && password != confirmacion ->
                        "Las contraseñas no coinciden"

                    else -> null
                }

                if (error != null) {
                    mensaje = error
                } else {
                    procesando = true
                    mensaje = ""

                    scope.launch {
                        try {
                            val resultado = if (registro) {
                                FirebaseAuthManager.registerUser(
                                    email,
                                    password
                                )
                            } else {
                                FirebaseAuthManager.loginUser(
                                    email,
                                    password
                                )
                            }

                            if (resultado.isSuccess) {
                                if (registro) {
                                    // La cuenta se creó. Volvemos al login.
                                    FirebaseAuth.getInstance().signOut()

                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate("jugada") {
                                        popUpTo(navController.graph.id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                mensaje = resultado.exceptionOrNull()?.message
                                    ?: "No se pudo completar la operación"
                            }
                        } finally {
                            procesando = false
                        }
                    }
                }
            }
        ) {
            Text(
                if (procesando) "Procesando..."
                else if (registro) "Registrarme"
                else "Ingresar"
            )
        }

        TextButton(
            enabled = !procesando,
            onClick = {
                navController.navigate(
                    if (registro) "login" else "register"
                ) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        ) {
            Text(
                if (registro) "Ya tengo cuenta: iniciar sesión"
                else "Crear una cuenta"
            )
        }
    }
}