package com.example.practicamoviles.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.practicamoviles.presentation.auth.AuthScreen
import com.example.practicamoviles.presentation.jugada.JugadaScreen
import com.example.practicamoviles.presentation.sorteo.SeguimientoScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    val inicio = remember {
        if (FirebaseAuth.getInstance().currentUser != null) {
            "jugada"
        } else {
            "login"
        }
    }

    NavHost(
        navController = navController,
        startDestination = inicio
    ) {
        composable("login") {
            AuthScreen(navController, registro = false)
        }

        composable("register") {
            AuthScreen(navController, registro = true)
        }

        composable("jugada") {
            JugadaScreen(navController)
        }

        composable("seguimiento") {
            SeguimientoScreen(navController)
        }
    }
}