package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
fun App() {
    MaterialTheme {
        val screenHeight = window.innerHeight
        val screenWidth = window.innerWidth

        val scope = rememberCoroutineScope()
        var config by remember { mutableStateOf("") }
        var buttonEnabled by remember { mutableStateOf(true) }
        var buttonText by remember { mutableStateOf("Simulate") }

        var errorMessageVisible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("Непредвиденная ошибка") }

        val localContext = LocalPlatformContext.current

        val imageLoader = remember(localContext) {
            ImageLoader.Builder(localContext)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()
        }

        var rttSvg by remember { mutableStateOf<ImageRequest?>(null) }
        var cwndSvg by remember { mutableStateOf<ImageRequest?>(null) }
        var rateSvg by remember { mutableStateOf<ImageRequest?>(null) }
        var packetReorderingSvg by remember { mutableStateOf<ImageRequest?>(null) }

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
                TextField(
                    value = config,
                    onValueChange = { newText -> config = newText },
                    label = { Text("Enter simulation config") },
                    modifier = Modifier.width(750.dp).height((screenHeight * 0.75).dp)
                )
                Button(
                    onClick = {
                        buttonEnabled = false
                        buttonText = "Simulating..."
                        scope.launch {
                            errorMessageVisible = false
                            val api = SkynonsClientApiImpl()
                            val response = api.simulate(config)
                            val result = response.resultOrNull()
                            buttonEnabled = true
                            buttonText = "Simulate"
                            if (result != null) {
                                rttSvg = ImageRequest.Builder(localContext).data(result.rtt.toByteArray()).build()
                                cwndSvg =
                                    ImageRequest.Builder(localContext).data(result.cwnd.toByteArray()).build()
                                rateSvg =
                                    ImageRequest.Builder(localContext).data(result.rate.toByteArray()).build()
                                packetReorderingSvg =
                                    ImageRequest.Builder(localContext).data(result.packetReordering.toByteArray())
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
                AnimatedVisibility(errorMessageVisible) {
                    Text(errorMessage, modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5)))
                }
            }
            Column(
                modifier = Modifier.safeContentPadding().width((screenWidth * 0.6).dp).padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (rttSvg != null && cwndSvg != null && rateSvg != null && packetReorderingSvg != null) {
                    Row {
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = rttSvg,
                                imageLoader = imageLoader,
                                contentDescription = "rtt graph",
                                modifier = Modifier.background(Color.White, RoundedCornerShape(7))
                            )
                            Text("RTT graph")
                        }
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = cwndSvg,
                                imageLoader = imageLoader,
                                contentDescription = "cwnd graph",
                                modifier = Modifier.background(Color.White, RoundedCornerShape(7))
                            )
                            Text("CWND graph")
                        }
                    }
                    Row {
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = rateSvg,
                                imageLoader = imageLoader,
                                contentDescription = "Rate graph",
                                modifier = Modifier.background(Color.White, RoundedCornerShape(7))
                            )
                            Text("Rate graph")
                        }
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = packetReorderingSvg,
                                imageLoader = imageLoader,
                                contentDescription = "Packet reordering graph",
                                modifier = Modifier.background(Color.White, RoundedCornerShape(7))
                            )
                            Text("Packet reordering graph")
                        }
                    }
                }
            }
        }
    }
}
