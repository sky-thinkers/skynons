package com.skythinkers.skynons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.skythinkers.skynons.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val addButtonColors = ButtonColors(
    Color(0.9f, 0.9f, 1f),
    Color(0f, 0f, 0f),
    Color(0.75f, 1f, 0.75f),
    Color(0.5f, 0.5f, 0.5f)
)

fun tryAddHost(
    hostName: ObjectId,
    scope: CoroutineScope,
    api: SkynonsClientApi,
    simulationId: SimulationId,
    onFailure: (String) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = api.addHost(simulationId, hostName)
            val error = result.errorOrNull()
            if (error != null) {
                onFailure("Could not add host: ${error.message}")
            } else {
                onSuccess()
            }
        } catch (e: Exception) {
            onFailure("Unexpected error: ${e.message}")
        }
    }
}

fun tryAddSwitch(
    switchName: ObjectId,
    scope: CoroutineScope,
    api: SkynonsClientApi,
    simulationId: SimulationId,
    onFailure: (String) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = api.addSwitch(simulationId, switchName)
            val error = result.errorOrNull()
            if (error != null) {
                onFailure("Could not add switch: ${error.message}")
            } else {
                onSuccess()
            }
        } catch (e: Exception) {
            onFailure("Unexpected error: ${e.message}")
        }
    }
}

fun tryAddLink(
    linkName: ObjectId,
    from: ObjectId,
    to: ObjectId,
    speed: SpeedString,
    scope: CoroutineScope,
    api: SkynonsClientApi,
    simulationId: SimulationId,
    onFailure: (String) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = api.addLink(simulationId, linkName, from, to, speed)
            val error = result.errorOrNull()
            if (error != null) {
                onFailure("Could not add link: ${error.message}")
            } else {
                onSuccess()
            }
        } catch (e: Exception) {
            onFailure("Unexpected error: ${e.message}")
        }
    }
}

fun tryAddConnection(
    connectionName: ObjectId,
    sender: ObjectId,
    receiver: ObjectId,
    size: SizeString,
    scope: CoroutineScope,
    api: SkynonsClientApi,
    simulationId: SimulationId,
    onFailure: (String) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = api.addConnection(simulationId, connectionName, sender, receiver, size)
            val error = result.errorOrNull()
            if (error != null) {
                onFailure("Could not add connection: ${error.message}")
            } else {
                onSuccess()
            }
        } catch (e: Exception) {
            onFailure("Unexpected error: ${e.message}")
        }
    }
}

@Composable
fun ImageRequest.toImage(description: String, width: Int, imageLoader: ImageLoader) {
    Column(
        modifier = Modifier.width((width * 0.2).dp).padding(10.dp),
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

@Composable
fun ObjectsList(objects: MutableList<String>, buttonText: String, onAddElement: () -> Unit) {
    Column(
        modifier = Modifier.padding(5.dp)
    ) {
        objects.show()
        Button(
            onClick = {
                onAddElement()
            },
            shape = RoundedCornerShape(10),
            colors = addButtonColors
        ) {
            Text(buttonText)
        }
    }
}