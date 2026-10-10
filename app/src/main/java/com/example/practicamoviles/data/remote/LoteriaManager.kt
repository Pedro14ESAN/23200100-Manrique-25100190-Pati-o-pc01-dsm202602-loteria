package com.example.practicamoviles.data.remote

import com.example.practicamoviles.data.model.JugadaModel
import com.example.practicamoviles.data.model.SorteoModel
import com.example.practicamoviles.domain.LoteriaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object LoteriaManager {

    const val SORTEO_ID = "sorteo_prueba"

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val sorteoRef
        get() = firestore.collection("sorteos").document(SORTEO_ID)

    // Recupera el nombre, la fecha y el estado del sorteo.
    suspend fun obtenerSorteo(): Result<SorteoModel> {
        return try {
            val documento = sorteoRef.get().await()

            if (!documento.exists()) {
                throw Exception("El sorteo todavía no ha sido creado")
            }

            val sorteo = SorteoModel(
                id = documento.id,
                nombre = documento.getString("nombre") ?: "",
                fecha = documento.getString("fecha") ?: "",
                estado = documento.getString("estado") ?: ""
            )

            Result.success(sorteo)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Registra una única jugada para el usuario autenticado.
    suspend fun guardarJugada(numeros: List<Int>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: throw Exception("Debes iniciar sesión")

            val numerosGuardar = numeros.toList()

            val error = LoteriaUtils.validarJugada(numerosGuardar)

            if (error != null) {
                throw Exception(error)
            }

            val jugadaRef = sorteoRef
                .collection("jugadas")
                .document(uid)

            firestore.runTransaction { transaction ->

                val sorteo = transaction.get(sorteoRef)
                val jugadaExistente = transaction.get(jugadaRef)

                if (!sorteo.exists()) {
                    throw Exception("El sorteo no existe")
                }

                if (sorteo.getString("estado") != "abierto") {
                    throw Exception(
                        "El sorteo ya no acepta nuevas jugadas"
                    )
                }

                if (jugadaExistente.exists()) {
                    throw Exception(
                        "Ya registraste una jugada para este sorteo"
                    )
                }

                val datos = hashMapOf(
                    "sorteoId" to SORTEO_ID,
                    "uid" to uid,
                    "numeros" to numerosGuardar
                )

                transaction.set(jugadaRef, datos)

                Unit
            }.await()

            Result.success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recupera únicamente la jugada del usuario de la sesión.
    suspend fun obtenerMiJugada(): Result<JugadaModel?> {
        return try {
            val uid = auth.currentUser?.uid
                ?: throw Exception("Debes iniciar sesión")

            val documento = sorteoRef
                .collection("jugadas")
                .document(uid)
                .get()
                .await()

            if (!documento.exists()) {
                Result.success(null)
            } else {
                val numerosFirestore =
                    documento.get("numeros") as? List<*>
                        ?: throw Exception("La jugada no tiene números")

                val numeros = numerosFirestore.map { valor ->
                    val numero = valor as? Long
                        ?: throw Exception(
                            "Los números guardados deben ser enteros"
                        )

                    if (numero !in 1L..45L) {
                        throw Exception(
                            "La jugada contiene un número fuera de rango"
                        )
                    }

                    numero.toInt()
                }

                val error = LoteriaUtils.validarJugada(numeros)

                if (error != null) {
                    throw Exception(error)
                }

                if (documento.getString("uid") != uid ||
                    documento.getString("sorteoId") != SORTEO_ID
                ) {
                    throw Exception(
                        "La jugada no corresponde al usuario o al sorteo"
                    )
                }

                val jugada = JugadaModel(
                    sorteoId = SORTEO_ID,
                    uid = uid,
                    numeros = numeros
                )

                Result.success(jugada)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}