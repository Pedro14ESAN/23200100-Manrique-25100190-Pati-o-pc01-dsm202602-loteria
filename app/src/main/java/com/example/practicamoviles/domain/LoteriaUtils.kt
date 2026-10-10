package com.example.practicamoviles.domain

object LoteriaUtils {

    fun validarJugada(numeros: List<Int>): String? {
        if (numeros.size != 6) {
            return "Debes ingresar exactamente seis números"
        }

        if (numeros.any { it < 1 || it > 45 }) {
            return "Los números deben estar entre 1 y 45"
        }

        if (numeros.distinct().size != 6) {
            return "Los números no deben repetirse"
        }

        return null
    }

    fun calcularAciertos(
        numerosJugada: List<Int>,
        numerosSorteados: List<Int>
    ): Int {
        return numerosJugada.distinct().count {
            it in numerosSorteados
        }
    }

    fun obtenerMensaje(
        estado: String,
        cantidadBolillas: Int,
        aciertos: Int
    ): String {
        return when (estado) {
            "abierto" -> "Esperando el inicio del sorteo"

            "en curso" -> "Sorteo en curso"

            "finalizado" -> {
                if (cantidadBolillas == 6) {
                    if (aciertos == 6) {
                        "GANADOR"
                    } else {
                        "SIGUE INTENTANDO"
                    }
                } else {
                    "Esperando resultados completos"
                }
            }

            else -> "Estado del sorteo desconocido"
        }
    }
}