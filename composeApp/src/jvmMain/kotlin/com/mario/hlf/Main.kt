package com.mario.hlf

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hundir la Flota"
    ) {
        App()
    }
}
