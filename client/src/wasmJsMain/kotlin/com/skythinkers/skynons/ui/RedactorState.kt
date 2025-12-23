package com.skythinkers.skynons.ui

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.skythinkers.skynons.api.*
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

@Stable
class RedactorState(
    val scope: CoroutineScope,
    val api: SkynonsClientApi,
    var simulationId: SimulationId,
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

    fun loadConfig(config: SimulationState, id: SimulationId) {
        hostsList.clear()
        switchesList.clear()
        linksList.clear()
        connectionsList.clear()
        hostsList.addAll(config.hosts)
        switchesList.addAll(config.switches)
        linksList.addAll(config.links)
        connectionsList.addAll(config.connections)
        simulationId = id
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
        hostName: ObjectId,
        onSuccess: () -> Unit,
    ) {
        apiAction {
            val result = api.addHost(simulationId, hostName)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add host: ${error.message}")
            } else {
                hostsList += Host(hostName)
                onSuccess()
            }
        }
    }

    fun tryAddSwitch(
        switchName: ObjectId,
        onSuccess: () -> Unit,
    ) {
        apiAction {
            val result = api.addSwitch(simulationId, switchName)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add switch: ${error.message}")
            } else {
                switchesList += Switch(switchName)
                onSuccess()
            }
        }
    }

    fun tryAddLink(
        linkName: ObjectId,
        from: ObjectId,
        to: ObjectId,
        speed: SpeedString,
        onSuccess: () -> Unit,
    ) {
        apiAction {
            val result = api.addLink(simulationId, linkName, from, to, speed)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add link: ${error.message}")
            } else {
                linksList += Link(linkName, from, to, speed)
                onSuccess()
            }
        }
    }

    fun tryAddConnection(
        connectionName: ObjectId,
        sender: ObjectId,
        receiver: ObjectId,
        size: SizeString,
        onSuccess: () -> Unit,
    ) {
        apiAction {
            val result = api.addConnection(simulationId, connectionName, sender, receiver, size)
            val error = result.errorOrNull()
            if (error != null) {
                showError("Could not add connection: ${error.message}")
            } else {
                connectionsList += Connection(connectionName, sender, receiver, size)
                onSuccess()
            }
        }
    }

    @Composable
    inline fun <reified T : NetworkObject> show() {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            val list = when (typeOf<T>()) {
                typeOf<Host>() -> hostsList.map { it.name to it.name }
                typeOf<Switch>() -> switchesList.map { it.name to it.name }
                typeOf<Link>() -> linksList.map { "${it.name}:\n${it.fromId} -> ${it.toId} (${it.speed})" to it.name }
                typeOf<Connection>() -> connectionsList.map { "${it.name}:\n${it.senderId} -> ${it.receiverId} (${it.sizeToSend})" to it.name }
                else -> emptyList<Nothing>()
            }
            list.forEach { (name, id) ->
                key(id) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        var text by remember { mutableStateOf(name) }
                        var deleteVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                when (interaction) {
                                    is HoverInteraction.Enter -> deleteVisible = true
                                    is HoverInteraction.Exit -> deleteVisible = false
                                }
                            }
                        }
                        Box(
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Button(
                                modifier = Modifier.padding(2.dp)
                                    .hoverable(interactionSource = interactionSource),
                                shape = RoundedCornerShape(10),
                                colors = ButtonColor.BLANK,
                                onClick = {
                                    tryRemoveObject(id)
                                }
                            ) {
                                Text(
                                    text = text
                                )
                            }
                            if (deleteVisible) {
                                Icon(
                                    Icons.Default.Close,
                                    "",
                                    modifier = Modifier.offset(3.dp, 0.dp).scale(0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    inline fun <reified T : NetworkObject> ObjectsList(buttonText: String, crossinline onAddElement: () -> Unit) {
        val scale = 1.5
        val w = when (typeOf<T>()) {
            typeOf<Host>() -> (window.innerWidth * 0.055 * scale).dp
            typeOf<Switch>() -> (window.innerWidth * 0.06 * scale).dp
            typeOf<Link>() -> (window.innerWidth * 0.05 * scale).dp
            else -> (window.innerWidth * 0.08 * scale).dp
        }
        Column(
            modifier = Modifier.padding(5.dp).width(w),
            horizontalAlignment = Alignment.CenterHorizontally
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