package com.example.practicamoviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.practicamoviles.presentation.navigation.AppNavGraph
import com.example.practicamoviles.ui.theme.PC1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PC1Theme {
                AppNavGraph()
            }
        }
    }
}