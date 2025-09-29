package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.skythinkers.skynons.api.SkynonsClientApiImpl
import com.skythinkers.skynons.api.errorOrNull
import com.skythinkers.skynons.api.resultOrNull
import io.ktor.utils.io.core.*
import kotlinx.browser.window
import kotlinx.coroutines.launch

@Composable
fun UserInputDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String,
    label: String,
    initialInput: String = ""
) {
    var textInput by remember { mutableStateOf(initialInput) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth() // Optional: make the TextField fill the dialog width
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(textInput)
                    onDismissRequest()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun App() {
    MaterialTheme {
        val screenHeight = window.innerHeight
        val screenWidth = window.innerWidth

        val localContext = LocalPlatformContext.current

        val imageLoader = remember(localContext) {
            ImageLoader.Builder(localContext)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()
        }

        @Composable
        fun ImageRequest.toImage(description: String) {
            Column(
                modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = this,
                    imageLoader = imageLoader,
                    contentDescription = description,
                    modifier = Modifier.background(Color.White, RoundedCornerShape(7))
                )
                Text(description)
            }
        }

        @Composable
        fun MutableList<String>.show() {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                this@show.forEach { name ->
                    Text(
                        modifier = Modifier.padding(5.dp).background(Color.White, RoundedCornerShape(35)),
                        text = " $name "
                    )
                }
            }
        }

        val scope = rememberCoroutineScope()
        val config by remember { mutableStateOf("") }
        var buttonEnabled by remember { mutableStateOf(true) }
        var buttonText by remember { mutableStateOf("Simulate") }

        var errorMessageVisible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("Непредвиденная ошибка") }

        var rttSvg by remember { mutableStateOf<ImageRequest?>(null) }
        var cwndSvg by remember { mutableStateOf<ImageRequest?>(null) }
        var rateSvg by remember { mutableStateOf<ImageRequest?>(null) }
        var packetReorderingSvg by remember { mutableStateOf<ImageRequest?>(null) }

        var showHostDialog by remember { mutableStateOf(false) }
        var showSwitchDialog by remember { mutableStateOf(false) }
        var showLinkDialog by remember { mutableStateOf(false) }
        var showConnectionDialog by remember { mutableStateOf(false) }

        val hostsList by remember { mutableStateOf<MutableList<String>>(mutableListOf()) }
        val switchesList by remember { mutableStateOf<MutableList<String>>(mutableListOf()) }
        val linksList by remember { mutableStateOf<MutableList<String>>(mutableListOf()) }
        val connectionsList by remember { mutableStateOf<MutableList<String>>(mutableListOf()) }

        val addButtonColors = ButtonColors(
            Color(0.9f, 0.9f, 1f),
            Color(0f, 0f, 0f),
            Color(0.75f, 1f, 0.75f),
            Color(0.5f, 0.5f, 0.5f)
        )

        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.safeContentPadding().width((screenWidth * 0.4).dp).padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Колонки объектов
                Row(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(2.dp))
                        .background(Color(0.8f, 0.8f, 0.8f),
                            RoundedCornerShape(2))
                ) {
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        hostsList.show()
                        Button(
                            onClick = {
                                showHostDialog = true
                            },
                            shape = RoundedCornerShape(10),
                            colors = addButtonColors
                        ) {
                            Text("Add host")
                        }
                    }
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        switchesList.show()
                        Button(
                            onClick = {
                                showSwitchDialog = true
                            },
                            shape = RoundedCornerShape(10),
                            colors = addButtonColors
                        ) {
                            Text("Add switch")
                        }
                    }
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        linksList.show()
                        Button(
                            onClick = {
                                showLinkDialog = true
                            },
                            shape = RoundedCornerShape(10),
                            colors = addButtonColors
                        ) {
                            Text("Add link")
                        }
                    }
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        connectionsList.show()
                        Button(
                            onClick = {
                                showConnectionDialog = true
                            },
                            shape = RoundedCornerShape(10),
                            colors = addButtonColors
                        ) {
                            Text("Add connection")
                        }
                    }
                }

                AnimatedVisibility(showHostDialog) {
                    UserInputDialog(
                        onDismissRequest = { showHostDialog = false },
                        onConfirm = { input ->
                            showHostDialog = false
                            hostsList += input
                        },
                        title = "Enter new host name",
                        label = "Name"
                    )
                }

                AnimatedVisibility(showSwitchDialog) {
                    UserInputDialog(
                        onDismissRequest = { showSwitchDialog = false },
                        onConfirm = { input ->
                            showSwitchDialog = false
                            switchesList += input
                        },
                        title = "Enter new switch name",
                        label = "Name"
                    )
                }

                AnimatedVisibility(showLinkDialog) {
                    UserInputDialog(
                        onDismissRequest = { showLinkDialog = false },
                        onConfirm = { input ->
                            showLinkDialog = false
                            linksList += input
                        },
                        title = "Enter new link name",
                        label = "Name"
                    )
                }

                AnimatedVisibility(showConnectionDialog) {
                    UserInputDialog(
                        onDismissRequest = { showConnectionDialog = false },
                        onConfirm = { input ->
                            showConnectionDialog = false
                            connectionsList += input
                        },
                        title = "Enter new connection name",
                        label = "Name"
                    )
                }

                // Кнопка старта симуляции
                Button(
                    onClick = {
                        buttonEnabled = false
                        buttonText = "Simulating..."
                        errorMessageVisible = false
                        val api = SkynonsClientApiImpl()
                        scope.launch {
                            val response = api.simulateConfig(config)
                            val result = response.resultOrNull()
                            buttonEnabled = true
                            buttonText = "Simulate"
                            if (result != null) {
                                rttSvg =
                                    ImageRequest.Builder(localContext).data(result.rtt.toByteArray()).build()
                                cwndSvg =
                                    ImageRequest.Builder(localContext).data(result.cwnd.toByteArray()).build()
                                rateSvg =
                                    ImageRequest.Builder(localContext).data(result.rate.toByteArray()).build()
                                packetReorderingSvg =
                                    ImageRequest.Builder(localContext)
                                        .data(result.packetReordering.toByteArray())
                                        .build()
                            } else {
                                errorMessage = response.errorOrNull()?.message ?: "Непредвиденная ошибка"
                                errorMessageVisible = true
                            }
                        }
                    },
                    enabled = buttonEnabled
                ) {
                    Text(buttonText)
                }

                // Сообщение об ошибке
                AnimatedVisibility(errorMessageVisible) {
                    SelectionContainer {
                        Text(errorMessage, modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5)))
                    }
                }
            }

            // Графы
            Column(
                modifier = Modifier.safeContentPadding().width((screenWidth * 0.6).dp).padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (rttSvg != null && cwndSvg != null && rateSvg != null && packetReorderingSvg != null) {
                    Row {
                        rttSvg!!.toImage("RTT graph")
                        cwndSvg!!.toImage("CWND graph")
                    }
                    Row {
                        rateSvg!!.toImage("Rate graph")
                        packetReorderingSvg!!.toImage("Packet reordering graph")
                    }
                }
            }
        }
    }
}
