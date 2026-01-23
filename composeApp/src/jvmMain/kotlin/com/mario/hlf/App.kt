package com.mario.hlf

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

@Composable
fun App(onExit: () -> Unit) {
    val state = rememberWindowState()

    Window(
        onCloseRequest = onExit,
        title = "Hundir la Flota",
        state = state
    ) {
        MaterialTheme {
            // TODO UI real
        }
    }
}
