package org.gendev25.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {

    Window(
        onCloseRequest = ::exitApplication,
        title = "GenDev25",
    ) {
        App()
    }
}