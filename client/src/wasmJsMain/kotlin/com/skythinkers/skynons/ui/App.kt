package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.skythinkers.skynons.Greeting
import com.skythinkers.skynons.api.SkynonsClientApiImpl
import com.skythinkers.skynons.api.resultOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

import skynons.client.generated.resources.Res
import skynons.client.generated.resources.compose_multiplatform

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var config by remember { mutableStateOf("Hello!") }
        var resultText by remember { mutableStateOf("Wait for result") }
        var buttonEnabled by remember { mutableStateOf(true) }
        var buttonText by remember { mutableStateOf("Simulate") }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextField(
                value = config,
                onValueChange = { newText -> config = newText },
                label = { Text("Enter simulation config") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                buttonEnabled = false
                buttonText = "Simulating..."
                scope.launch {
                    val api = SkynonsClientApiImpl()
                    buttonText = "Simulating... 1"
                    val result = api.simulate(config)
                    buttonText = "Simulating... 2"
                    resultText = result.resultOrNull().toString()
                    buttonText = "Simulating... 3"
                    buttonEnabled = true
                    buttonText = "Simulate"
                }
            },
                enabled = buttonEnabled) {
                Text(buttonText)
            }
            Text(resultText)
        }
    }
}
