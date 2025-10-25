package com.skythinkers.skynons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.skythinkers.skynons.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ButtonColor {
    val ADD = ButtonColors(
        Color(0.9f, 0.9f, 1f),
        Color(0f, 0f, 0f),
        Color(0.75f, 1f, 0.75f),
        Color(0.5f, 0.5f, 0.5f)
    )

    val DELETE = ButtonColors(
        Color(1.0f, 0.7f, 0.7f),
        Color(0f, 0f, 0f),
        Color(0.1f, 0.75f, 0.75f),
        Color(0.5f, 0.5f, 0.5f)
    )
}

fun tryRemoveObject(
    id: ObjectId,
    scope: CoroutineScope,
    api: SkynonsClientApi,
    simulationId: SimulationId,
    onFailure: (String) -> Unit,
    onSuccess: (List<ObjectId>) -> Unit
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = api.removeObject(simulationId, id)
            val objects = result.resultOrNull()
            if (objects != null) {
                onSuccess(objects)
            } else {
                val error = result.errorOrNull()
                if (error != null) {
                    onFailure("Could not remove object: ${error.message}")
                } else {
                    onFailure("Unrecognized error")
                }
            }
        } catch (e: Exception) {
            onFailure("Unexpected error: ${e.message}")
        }
    }
}

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
fun List<*>.show(onDeleteButtonClicked: (ObjectId) -> Unit) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        this@show.forEach { obj ->
            var name = ""
            var id = ""
            if (obj is Host) {
                name = obj.name
                id = obj.name
            }
            if (obj is Switch) {
                name = obj.name
                id = obj.name
            }
            if (obj is Link) {
                name = "${obj.name}: ${obj.fromId} -> ${obj.toId} (${obj.speed})"
                id = obj.name
            }
            if (obj is Connection) {
                name = "${obj.name}: ${obj.senderId} -> ${obj.receiverId} (${obj.sizeToSend})"
                id = obj.name
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(5.dp).background(Color.White, RoundedCornerShape(35)),
                    text = " $name "
                )
                Button(
                    modifier = Modifier.padding(2.dp).size(26.dp, 18.dp),
                    shape = RoundedCornerShape(10),
                    colors = ButtonColor.DELETE,
                    onClick = {
                        onDeleteButtonClicked(id)
                    }
                ) {
                    Text(
                        text = "-"
                    )
                }
            }
        }
    }
}

@Composable
fun ObjectsList(objects: List<*>, buttonText: String, onAddElement: () -> Unit, onDeleteElement: (ObjectId) -> Unit) {
    Column(
        modifier = Modifier.padding(5.dp)
    ) {
        objects.show(onDeleteElement)
        Button(
            onClick = {
                onAddElement()
            },
            shape = RoundedCornerShape(10),
            colors = ButtonColor.ADD
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