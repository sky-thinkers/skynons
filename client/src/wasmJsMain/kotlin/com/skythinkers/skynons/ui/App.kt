package com.skythinkers.skynons.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val pageManager by remember { mutableStateOf(PageManager(scope)) }

        pageManager.showPage()
    }
}
