package com.example.practicamoviles.presentation.sorteo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.practicamoviles.data.model.BolillaModel
import com.example.practicamoviles.data.model.JugadaModel
import com.example.practicamoviles.data.model.SorteoModel
import com.example.practicamoviles.data.remote.LoteriaManager
import com.example.practicamoviles.domain.LoteriaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SeguimientoScreen(navController: NavController) {

    val usuario = FirebaseAuth.getInstance().currentUser

    var sorteo by remember {
        mutableStateOf<SorteoModel?>(null)
    }

    var jugada by remember(usuario?.uid) {
        mutableStateOf<JugadaModel?>(null)
    }

    var bolillas by remember {
        mutableStateOf<List<BolillaModel>>(emptyList())
    }

    var cargandoJugada by remember { mutableStateOf(true) }
    var cargandoBolillas by remember { mutableStateOf(true) }

    var errorJugada by remember { mutableStateOf<String?>(null) }
    var errorSorteo by remember { mutableStateOf<String?>(null) }
    var errorBolillas by remember { mutableStateOf<String?>(null) }

    var intento by remember { mutableStateOf(0) }

    // Recuperar la jugada almacenada del usuario actual.
    LaunchedEffect(usuario?.uid, intento) {
        cargandoJugada = true
        errorJugada = null
        jugada = null

        try {
            val resultado = LoteriaManager.obtenerMiJugada()

            if (resultado.isSuccess) {
                jugada = resultado.getOrNull()
            } else {
                errorJugada = resultado.exceptionOrNull()?.message
                    ?: "No se pudo recuperar tu jugada"
            }
        } finally {
            cargandoJugada = false
        }
    }

    // Registrar las dos escuchas de Firestore.
    DisposableEffect(usuario?.uid, intento) {

        sorteo = null
        bolillas = emptyList()
        cargandoBolillas = true
        errorSorteo = null
        errorBolillas = null

        val referencia = FirebaseFirestore.getInstance()
            .collection("sorteos")
            .document(LoteriaManager.SORTEO_ID)

        // Listener 1: información y estado del sorteo.
        val listenerSorteo = referencia.addSnapshotListener {
                documento, error ->

            if (error != null) {
                sorteo = null
                errorSorteo = error.message
                    ?: "No se pudo escuchar el sorteo"

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
                errorSorteo = "El sorteo no existe"
            }
        }

        // Listener 2: bolillas publicadas.
        val listenerBolillas = referencia
            .collection("bolillas")
            .addSnapshotListener { resultado, error ->

                cargandoBolillas = false

                if (error != null) {
                    bolillas = emptyList()
                    errorBolillas = error.message
                        ?: "No se pudieron escuchar las bolillas"

                } else if (resultado != null) {
                    try {
                        val recibidas = resultado.documents.map { documento ->

                            val numero = documento.get("numero") as? Long
                                ?: throw Exception(
                                    "La bolilla ${documento.id} necesita numero entero"
                                )

                            val orden = documento.get("orden") as? Long
                                ?: throw Exception(
                                    "La bolilla ${documento.id} necesita orden entero"
                                )

                            if (numero !in 1L..45L || orden !in 1L..6L) {
                                throw Exception(
                                    "Bolilla fuera de rango: ${documento.id}"
                                )
                            }

                            BolillaModel(
                                numero = numero.toInt(),
                                orden = orden.toInt()
                            )
                        }.sortedBy { it.orden }

                        if (recibidas.size > 6) {
                            throw Exception("Hay más de seis bolillas")
                        }

                        if (recibidas.map { it.numero }.distinct().size
                            != recibidas.size
                        ) {
                            throw Exception("Hay números de bolilla repetidos")
                        }

                        val ordenesEsperados =
                            (1..recibidas.size).toList()

                        if (recibidas.map { it.orden } != ordenesEsperados) {
                            throw Exception(
                                "Los órdenes deben ser consecutivos desde 1"
                            )
                        }

                        bolillas = recibidas
                        errorBolillas = null

                    } catch (e: Exception) {
                        bolillas = emptyList()
                        errorBolillas = e.message
                            ?: "Los datos de las bolillas no son válidos"
                    }
                } else {
                    bolillas = emptyList()
                    errorBolillas = "No se recibieron datos de bolillas"
                }
            }

        // Retirar ambas escuchas al salir de esta pantalla.
        onDispose {
            listenerSorteo.remove()
            listenerBolillas.remove()
        }
    }

    val numerosJugada = jugada?.numeros ?: emptyList()
    val numerosSorteados = bolillas.map { it.numero }

    val aciertos = LoteriaUtils.calcularAciertos(
        numerosJugada,
        numerosSorteados
    )

    val hayError = errorJugada != null ||
            errorSorteo != null ||
            errorBolillas != null

    val datosListos = !cargandoJugada &&
            !cargandoBolillas &&
            sorteo != null &&
            jugada != null &&
            !hayError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Seguimiento del sorteo",
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

        errorSorteo?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        errorJugada?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        errorBolillas?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (hayError) {
            Button(onClick = { intento++ }) {
                Text("Reintentar")
            }
        }

        Text(
            text = "Mi jugada",
            style = MaterialTheme.typography.titleMedium
        )

        if (cargandoJugada) {
            Text("Recuperando tu jugada...")
        } else if (jugada == null && errorJugada == null) {
            Text("Todavía no tienes una jugada registrada.")
        }

        numerosJugada.forEach { numero ->

            val acertado = numero in numerosSorteados

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (acertado) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = if (acertado) {
                        "$numero — ACERTADO"
                    } else {
                        "$numero — Sin coincidencia"
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Text(
            text = "Bolillas en orden de salida",
            style = MaterialTheme.typography.titleMedium
        )

        if (cargandoBolillas) {
            Text("Cargando bolillas...")
        } else if (errorBolillas == null) {
            Text("Bolillas publicadas: ${bolillas.size} de 6")

            if (bolillas.isEmpty()) {
                Text("Todavía no se han publicado bolillas.")
            }

            bolillas.forEach { bolilla ->
                Text("${bolilla.orden}. Número ${bolilla.numero}")
            }
        }

        if (datosListos) {
            Text(
                text = "Aciertos: $aciertos de 6",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = LoteriaUtils.obtenerMensaje(
                    estado = sorteo!!.estado,
                    cantidadBolillas = bolillas.size,
                    aciertos = aciertos
                ),
                style = MaterialTheme.typography.headlineSmall
            )
        } else if (!hayError && sorteo == null) {
            Text("Cargando información del sorteo...")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val regreso = navController.popBackStack(
                    "jugada",
                    false
                )

                if (!regreso) {
                    navController.navigate("jugada") {
                        popUpTo("seguimiento") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        ) {
            Text("Volver a mi jugada")
        }
    }

}