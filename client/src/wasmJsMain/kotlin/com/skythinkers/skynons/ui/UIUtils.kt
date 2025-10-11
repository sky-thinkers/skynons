package com.skythinkers.skynons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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