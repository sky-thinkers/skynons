package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.skythinkers.skynons.api.SkynonsClientApiImpl
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

        var rttSvg by remember { mutableStateOf<String?>(null) }
        var cwndSvg by remember { mutableStateOf<String?>(null) }
        var rateSvg by remember { mutableStateOf<String?>(null) }
        var packetReorderingSvg by remember { mutableStateOf<String?>(null) }
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
                            val api = SkynonsClientApiImpl()
                            val result = api.simulate(config).resultOrNull()
                            buttonEnabled = true
                            buttonText = "Simulate"
                            if (result != null) {
                                rttSvg = result.rtt.data
                                cwndSvg = result.cwnd.data
                                rateSvg = result.rate.data
                                packetReorderingSvg = result.packetReordering.data
                            }
                        }
                    },
                    enabled = buttonEnabled
                ) {
                    Text(buttonText)
                }
            }
            Column(
                modifier = Modifier.safeContentPadding().width((screenWidth * 0.6).dp).padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val imageLoader = ImageLoader.Builder(LocalPlatformContext.current)
                    .components {
                        add(SvgDecoder.Factory())
                    }
                    .build()
                if (rttSvg != null && cwndSvg != null && rateSvg != null && packetReorderingSvg != null) {
                    Row {
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(rttSvg!!.toByteArray())
                                    .build(), imageLoader = imageLoader,
                                contentDescription = "rtt graph",
                                modifier = Modifier.background(Color.White)
                            )
                            Text("RTT graph")
                        }
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(cwndSvg!!.toByteArray())
                                    .build(), imageLoader = imageLoader,
                                contentDescription = "cwnd graph",
                                modifier = Modifier.background(Color.White)
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
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(rateSvg!!.toByteArray())
                                    .build(), imageLoader = imageLoader,
                                contentDescription = "Rate graph",
                                modifier = Modifier.background(Color.White)
                            )
                            Text("Rate graph")
                        }
                        Column(
                            modifier = Modifier.width((screenWidth * 0.2).dp).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(packetReorderingSvg!!.toByteArray())
                                    .build(), imageLoader = imageLoader,
                                contentDescription = "Packet reordering graph",
                                modifier = Modifier.background(Color.White)
                            )
                            Text("Packet reordering graph")
                        }
                    }
                }
            }
        }
    }
}
