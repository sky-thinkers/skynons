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
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.skythinkers.skynons.api.*
import io.ktor.utils.io.core.*
import kotlinx.browser.window
import kotlinx.coroutines.*

@Composable
fun SingleInputDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String,
    label: String,
    initialInput: String = "",
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
                modifier = Modifier.fillMaxWidth()
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
fun FourInputsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    title: String,
    label1: String,
    label2: String,
    label3: String,
    label4: String,
    initialInput: String = "",
) {
    var input1 by remember { mutableStateOf(initialInput) }
    var input2 by remember { mutableStateOf(initialInput) }
    var input3 by remember { mutableStateOf(initialInput) }
    var input4 by remember { mutableStateOf(initialInput) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = input1,
                    onValueChange = { input1 = it },
                    label = { Text(label1) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp)
                )
                TextField(
                    value = input2,
                    onValueChange = { input2 = it },
                    label = { Text(label2) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp)
                )
                TextField(
                    value = input3,
                    onValueChange = { input3 = it },
                    label = { Text(label3) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp)
                )
                TextField(
                    value = input4,
                    onValueChange = { input4 = it },
                    label = { Text(label4) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(input1, input2, input3, input4)
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

        val localContext = LocalPlatformContext.current
        val scope = rememberCoroutineScope()

        val api = remember(localContext) { SkynonsClientApiImpl() }

        var simulationId by remember(localContext) { mutableStateOf<String?>(null) }

        var connectionError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(api) {
            val resp = api.createSimulation()
            val result = resp.resultOrNull()
            if (result == null) {
                connectionError =
                    "Не удалось подключиться к серверу: ${resp.errorOrNull()?.message ?: "Неопознанная ошибка"}"
                //TODO(This is for testing purposes)
                //simulationId = "1"
            } else {
                simulationId = result
            }
        }

        if (simulationId == null) {
            Text("Подключение к серверу... $connectionError")
        } else {
            GraphRedactor(localContext, scope, api, simulationId!!)
        }
    }
}

@Composable
fun GraphRedactor(
    localContext: PlatformContext,
    scope: CoroutineScope,
    api: SkynonsClientApi,
    simulationId: SimulationId
) {
    val screenHeight = window.innerHeight
    val screenWidth = window.innerWidth

    val imageLoader = remember(localContext) {
        ImageLoader.Builder(localContext)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

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
                    .background(
                        Color(0.8f, 0.8f, 0.8f),
                        RoundedCornerShape(2)
                    )
            ) {
                ObjectsList(hostsList, "Add host") {
                    showHostDialog = true
                }
                ObjectsList(switchesList, "Add switch") {
                    showSwitchDialog = true
                }
                ObjectsList(linksList, "Add link") {
                    showLinkDialog = true
                }
                ObjectsList(connectionsList, "Add connection") {
                    showConnectionDialog = true
                }
            }

            // Диалоги
            if (showHostDialog) {
                SingleInputDialog(
                    onDismissRequest = { showHostDialog = false },
                    onConfirm = { input ->
                        showHostDialog = false
                        tryAddHost(
                            input, scope, api, simulationId,
                            onFailure = { message ->
                                errorMessage = message
                                errorMessageVisible = true
                            },
                            onSuccess = {
                                hostsList += input
                            })
                    },
                    title = "Enter new host name",
                    label = "Name"
                )
            }

            if (showSwitchDialog) {
                SingleInputDialog(
                    onDismissRequest = { showSwitchDialog = false },
                    onConfirm = { input ->
                        showSwitchDialog = false
                        tryAddSwitch(
                            input, scope, api, simulationId,
                            onFailure = { message ->
                                errorMessage = message
                                errorMessageVisible = true
                            },
                            onSuccess = {
                                switchesList += input
                            })
                    },
                    title = "Enter new switch name",
                    label = "Name"
                )
            }

            if (showLinkDialog) {
                FourInputsDialog(
                    onDismissRequest = { showLinkDialog = false },
                    onConfirm = { name, from, to, speed ->
                        showLinkDialog = false
                        tryAddLink(
                            name, from, to, speed, scope, api, simulationId,
                            onFailure = { message ->
                                errorMessage = message
                                errorMessageVisible = true
                            },
                            onSuccess = {
                                linksList += "$name ($from -> $to, $speed)"
                            })
                    },
                    title = "Enter new link data",
                    label1 = "Link's name",
                    label2 = "First linked device name",
                    label3 = "Second linked device name",
                    label4 = "Link's speed"
                )
            }

            if (showConnectionDialog) {
                FourInputsDialog(
                    onDismissRequest = { showConnectionDialog = false },
                    onConfirm = { name, sender, receiver, size ->
                        tryAddConnection(
                            name, sender, receiver, size, scope, api, simulationId,
                            onFailure = { message ->
                                errorMessage = message
                                errorMessageVisible = true
                            },
                            onSuccess = {
                                connectionsList += "$name ($sender -> $receiver, $size)"
                            })
                    },
                    title = "Enter new connection data",
                    label1 = "Connection name",
                    label2 = "Sender name",
                    label3 = "Receiver name",
                    label4 = "Size of the transferred data"
                )
            }

            // Кнопка старта симуляции
            Button(
                onClick = {
                    buttonEnabled = false
                    buttonText = "Simulating..."
                    errorMessageVisible = false
                    scope.launch(Dispatchers.Default) {
                        try {
                            val response = api.simulate(simulationId)
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
                        } catch (e: Exception) {
                            errorMessage = "Непредвиденная ошибка: ${e.message}"
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
                    Text(
                        errorMessage,
                        modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                    )
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
                    rttSvg!!.toImage("RTT graph", screenWidth, imageLoader)
                    cwndSvg!!.toImage("CWND graph", screenWidth, imageLoader)
                }
                Row {
                    rateSvg!!.toImage("Rate graph", screenWidth, imageLoader)
                    packetReorderingSvg!!.toImage("Packet reordering graph", screenWidth, imageLoader)
                }
            }
        }
    }
}