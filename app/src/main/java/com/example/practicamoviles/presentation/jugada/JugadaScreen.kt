package com.example.practicamoviles.presentation.jugada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.practicamoviles.data.model.JugadaModel
import com.example.practicamoviles.data.model.SorteoModel
import com.example.practicamoviles.data.remote.LoteriaManager
import com.example.practicamoviles.domain.LoteriaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun JugadaScreen(navController: NavController) {

    val usuario = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var campos by remember(usuario?.uid) {
        mutableStateOf(List(6) { "" })
    }

    var sorteo by remember {
        mutableStateOf<SorteoModel?>(null)
    }

    var jugada by remember(usuario?.uid) {
        mutableStateOf<JugadaModel?>(null)
    }

    var cargandoJugada by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var errorJugada by remember { mutableStateOf<String?>(null) }
    var errorSorteo by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf("") }
    var intentoCarga by remember { mutableStateOf(0) }

    // Recuperar la jugada del usuario al entrar.
    LaunchedEffect(usuario?.uid, intentoCarga) {
        cargandoJugada = true
        errorJugada = null

        try {
            val resultado = LoteriaManager.obtenerMiJugada()

            if (resultado.isSuccess) {
                jugada = resultado.getOrNull()

                campos = jugada?.numeros?.map { it.toString() }
                    ?: List(6) { "" }
            } else {
                errorJugada = resultado.exceptionOrNull()?.message
                    ?: "No se pudo recuperar la jugada"
            }
        } finally {
            cargandoJugada = false
        }
    }

    // Escuchar el estado para bloquear el formulario al iniciar.
    DisposableEffect(usuario?.uid, intentoCarga) {
        sorteo = null
        errorSorteo = null

        val listener = FirebaseFirestore.getInstance()
            .collection("sorteos")
            .document(LoteriaManager.SORTEO_ID)
            .addSnapshotListener { documento, error ->

                if (error != null) {
                    sorteo = null
                    errorSorteo = error.message
                        ?: "No se pudo consultar el sorteo"
                } else if (documento != null && documento.exists()) {
                    errorSorteo = null

                    sorteo = SorteoModel(
                        id = documento.id,
                        nombre = documento.getString("nombre") ?: "",
                        fecha = documento.getString("fecha") ?: "",
                        estado = documento.getString("estado") ?: ""
                    )
                } else {
                    sorteo = null
                    errorSorteo = "El sorteo todavía no existe"
                }
            }

        onDispose {
            listener.remove()
        }
    }

    val puedeRegistrar =
        usuario != null &&
                !cargandoJugada &&
                !guardando &&
                errorJugada == null &&
                errorSorteo == null &&
                jugada == null &&
                sorteo?.estado == "abierto"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Mi jugada",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Usuario: ${usuario?.email ?: "Sin sesión"}")

        sorteo?.let {
            Text(
                text = it.nombre,
                style = MaterialTheme.typography.titleLarge
            )
            Text("Fecha: ${it.fecha}")
            Text("Estado: ${it.estado}")
        }

        if (sorteo == null && errorSorteo == null) {
            Text("Cargando sorteo...")
        }

        if (cargandoJugada) {
            Text("Recuperando tu jugada...")
        }

        errorSorteo?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        errorJugada?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (errorSorteo != null || errorJugada != null) {
            Button(
                onClick = { intentoCarga++ },
                enabled = !cargandoJugada && !guardando
            ) {
                Text("Reintentar")
            }
        }

        Text("Selecciona seis números distintos entre 1 y 45")

        campos.forEachIndexed { indice, valor ->
            OutlinedTextField(
                value = valor,
                onValueChange = { nuevoTexto ->
                    campos = campos.mapIndexed { posicion, anterior ->
                        if (posicion == indice) nuevoTexto else anterior
                    }
                    mensaje = ""
                },
                label = { Text("Número ${indice + 1}") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                enabled = puedeRegistrar,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (jugada != null) {
            Text("Tu jugada está registrada.")
        } else if (
            sorteo != null &&
            sorteo?.estado != "abierto" &&
            !cargandoJugada
        ) {
            Text("El sorteo ya no permite registrar jugadas.")
        }

        if (mensaje.isNotBlank()) {
            Text(mensaje)
        }

        Button(
            enabled = puedeRegistrar,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val numeros = campos.mapNotNull {
                    it.trim().toIntOrNull()
                }

                val error = if (numeros.size != 6) {
                    "Completa los seis campos con números enteros"
                } else {
                    LoteriaUtils.validarJugada(numeros)
                }

                if (error != null) {
                    mensaje = error
                } else {
                    guardando = true
                    mensaje = ""

                    scope.launch {
                        try {
                            val resultado =
                                LoteriaManager.guardarJugada(numeros)

                            if (resultado.isSuccess) {
                                jugada = JugadaModel(
                                    sorteoId = LoteriaManager.SORTEO_ID,
                                    uid = usuario!!.uid,
                                    numeros = numeros
                                )

                                mensaje = "Jugada guardada correctamente"
                            } else {
                                mensaje = resultado.exceptionOrNull()?.message
                                    ?: "No se pudo guardar la jugada"
                            }
                        } finally {
                            guardando = false
                        }
                    }
                }
            }
        ) {
            Text(if (guardando) "Guardando..." else "Guardar jugada")
        }

        Button(
            enabled = jugada != null &&
                    !cargandoJugada &&
                    !guardando &&
                    errorJugada == null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                navController.navigate("seguimiento") {
                    launchSingleTop = true
                }
            }
        ) {
            Text("Ver seguimiento")
        }

        TextButton(
            enabled = !guardando,
            onClick = {
                FirebaseAuth.getInstance().signOut()

                navController.navigate("login") {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        ) {
            Text("Cerrar sesión y volver al login")
        }
    }
}