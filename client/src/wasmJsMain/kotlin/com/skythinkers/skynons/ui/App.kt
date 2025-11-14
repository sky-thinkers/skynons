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
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.skythinkers.skynons.api.*
import io.ktor.utils.io.core.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*

@Composable
fun App() {
    MaterialTheme {

        val localContext = LocalPlatformContext.current
        val scope = rememberCoroutineScope()

        var api by remember { mutableStateOf<SkynonsClientApi>(SkynonsClientApiImpl("")) }

        var simulationId by remember { mutableStateOf<String?>(null) }

        var connectionError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(api) {
            runCatching {
                // TODO: check and reuse simulation from window.location.hash
                val resp = api.createSimulation()
                val result = resp.resultOrNull()
                if (result == null) {
                    connectionError =
                        "Не удалось подключиться к серверу: ${resp.errorOrNull()?.message ?: "Неопознанная ошибка"}"
                } else {
                    window.location.hash = result
                    simulationId = result
                }
            }.onFailure {
                connectionError =
                    "Не удалось подключиться к серверу: ${it.message ?: "Неопознанная ошибка"}"
            }
        }
        SideEffect {
            document.onkeydown = handler@{ event ->
                if (!event.ctrlKey) return@handler
                when (event.key) {
                    "l", "L" -> {
                        println("local")
                        simulationId = null
                        api = SkynonsClientApiMock()
                    }

                    "e", "E" -> {
                        simulationId = null
                        api = SkynonsClientApiImpl("")
                    }

                    "d", "D" -> {
                        simulationId = null
                        api = SkynonsClientApiImpl()
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val simulationId = simulationId
            val connectionError = connectionError
            when {
                simulationId != null -> GraphRedactor(localContext, scope, api, simulationId)
                connectionError != null -> Text("Не удалось подключиться к серверу $connectionError")
                else -> {
                    Text("Подключение к серверу...")
                    CircularProgressIndicator()
                }
            }
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
//    val screenHeight = window.innerHeight
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

    var rttSvg by remember { mutableStateOf<ImageRequest?>(null) }
    var cwndSvg by remember { mutableStateOf<ImageRequest?>(null) }
    var rateSvg by remember { mutableStateOf<ImageRequest?>(null) }
    var packetReorderingSvg by remember { mutableStateOf<ImageRequest?>(null) }

    var showHostDialog by remember { mutableStateOf(false) }
    var showSwitchDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }

    val state = remember { RedactorState(scope, api, simulationId) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.weight(0.4f).padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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
                state.ObjectsList<Host>("Add host") {
                    showHostDialog = true
                }
                state.ObjectsList<Switch>("Add switch") {
                    showSwitchDialog = true
                }
                state.ObjectsList<Link>("Add link") {
                    showLinkDialog = true
                }
                state.ObjectsList<Connection>("Add connection") {
                    showConnectionDialog = true
                }
            }

            // Диалоги
            if (showHostDialog) {
                SingleInputDialog(
                    onDismissRequest = { showHostDialog = false },
                    onConfirm = { input ->
                        showHostDialog = false
                        state.tryAddHost(input)
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
                        state.tryAddSwitch(input)
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
                        state.tryAddLink(name, from, to, speed)
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
                        state.tryAddConnection(name, sender, receiver, size)
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
                    state.clearError()
                    state.apiAction {
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
                            state.showError(response.errorOrNull()?.message ?: "Непредвиденная ошибка")
                        }
                    }
                },
                enabled = buttonEnabled
            ) {
                Text(buttonText)
            }

            // Сообщение об ошибке
            AnimatedVisibility(state.errorMessageVisible) {
                SelectionContainer {
                    Text(
                        state.errorMessage,
                        modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                    )
                }
            }
        }

        // Графы
        Column(
            modifier = Modifier.weight(0.6f).padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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
