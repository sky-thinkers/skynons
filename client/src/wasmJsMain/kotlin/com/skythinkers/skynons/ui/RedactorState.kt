package com.skythinkers.skynons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skythinkers.skynons.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

@Stable
class RedactorState(
    val scope: CoroutineScope,
    val api: SkynonsClientApi,
    val simulationId: SimulationId,
) {
    val hostsList = mutableStateListOf<Host>()
    val switchesList = mutableStateListOf<Switch>()
    val linksList = mutableStateListOf<Link>()
    val connectionsList = mutableStateListOf<Connection>()

    var errorMessageVisible by mutableStateOf(false)
    var errorMessage by mutableStateOf("Непредвиденная ошибка")

    private fun removeDevice(id: ObjectId) {
        hostsList.retainAll { it.name != id }
        switchesList.retainAll { it.name != id }
        linksList.retainAll { it.name != id }
        connectionsList.retainAll { it.name != id }
    }

    fun clearError() {
        errorMessageVisible = false
    }

    fun showError(message: String) {
        errorMessage = message
        errorMessageVisible = true
    }

    fun apiAction(action: suspend () -> Unit) {
        scope.launch(Dispatchers.Default) {
            try {
                action()
            } catch (e: Exception) {
                showError("Unexpected error: ${e.message}")
            }
        }
    }

    fun tryRemoveObject(id: ObjectId) {
        apiAction {
            val result = api.removeObject(simulationId, id)
            val objects = result.resultOrNull()
            if (objects != null) {
                for (obj in objects) {
                    removeDevice(obj)
                }
            } else {
                val error = result.errorOrNull()
                if (error != null) {
                    showError("Could not remove object: ${error.message}")
                } else {
                    showError("Unrecognized error")
                }
            }
        }
    }

    fun tryAddHost(
        hostName: ObjectId
    ) {
        apiAction {
            val result = api.addHost(simulationId, hostName)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add host: ${error.message}")
            } else {
                hostsList += Host(hostName)
            }
        }
    }

    fun tryAddSwitch(switchName: ObjectId) {
        apiAction {
            val result = api.addSwitch(simulationId, switchName)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add switch: ${error.message}")
            } else {
                switchesList += Switch(switchName)
            }
        }
    }

    fun tryAddLink(
        linkName: ObjectId,
        from: ObjectId,
        to: ObjectId,
        speed: SpeedString
    ) {
        apiAction {
            val result = api.addLink(simulationId, linkName, from, to, speed)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add link: ${error.message}")
            } else {
                linksList += Link(linkName, from, to, speed)
            }
        }
    }

    fun tryAddConnection(
        connectionName: ObjectId,
        sender: ObjectId,
        receiver: ObjectId,
        size: SizeString
    ) {
        apiAction {
            val result = api.addConnection(simulationId, connectionName, sender, receiver, size)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add connection: ${error.message}")
            } else {
                connectionsList += Connection(connectionName, sender, receiver, size)
            }
        }
    }

    @Composable
    inline fun <reified T : NetworkObject> show() {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            val list = when(typeOf<T>()) {
                typeOf<Host>() -> hostsList.map { it.name to it.name }
                typeOf<Switch>() -> switchesList.map { it.name to it.name }
                typeOf<Link>() -> linksList.map { "${it.name}: ${it.fromId} -> ${it.toId} (${it.speed})" to it.name }
                typeOf<Connection>() -> connectionsList.map { "${it.name}: ${it.senderId} -> ${it.receiverId} (${it.sizeToSend})" to it.name }
                else -> emptyList<Nothing>()
            }
            list.forEach { (name, id) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    var text by remember { mutableStateOf(" $name ") }
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is HoverInteraction.Enter -> text = "Удалить"
                                is HoverInteraction.Exit -> text = " $name "
                            }
                        }
                    }
                    Text(
                        modifier = Modifier.padding(5.dp).background(Color.White, RoundedCornerShape(35)),
                        text = text
                    )
                    Button(
                        modifier = Modifier.padding(2.dp).size(26.dp, 18.dp).hoverable(interactionSource = interactionSource),
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.DELETE,
                        onClick = {
                            tryRemoveObject(id)
                        }
                    ) {}
                }
            }
        }
    }

    @Composable
    inline fun <reified T: NetworkObject> ObjectsList(buttonText: String, crossinline onAddElement: () -> Unit) {
        Column(
            modifier = Modifier.padding(5.dp)
        ) {
            show<T>()
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
}