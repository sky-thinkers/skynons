package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

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

    val BLANK = ButtonColors(
        Color(1.0f, 1.0f, 1.0f),
        Color(0.0f, 0.0f, 0.0f),
        Color(0.7f, 0.7f, 0.7f),
        Color(0.0f, 0.0f, 0.0f)
    )
}

@Composable
fun ImageRequest.toImage(description: String, modifier: Modifier, imageLoader: ImageLoader, onClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = this@toImage,
            imageLoader = imageLoader,
            contentDescription = description,
            modifier = Modifier.background(Color.White, RoundedCornerShape(7)).clickable(onClick = onClick)
        )
        Text(description)
    }
}

@Composable
fun SingleInputDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String,
    label: String,
    initialInput: String = "",
    errorMessage: String?,
) {
    val focusRequester = remember { FocusRequester() }
    var textInput by remember { mutableStateOf(initialInput) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onConfirm(textInput)
                        }
                    )
                )
                AnimatedVisibility(errorMessage != null) {
                    SelectionContainer {
                        Text(
                            errorMessage!!,
                            modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(textInput)
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Deprecated("No longer used")
@Composable
fun TwoInputsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String) -> Unit,
    title: String,
    label1: String,
    label2: String,
    initialInput: String = "",
) {
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    var input1 by remember { mutableStateOf(initialInput) }
    var input2 by remember { mutableStateOf(initialInput) }

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
                    modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester1),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusRequester2.requestFocus()
                        }
                    )
                )
                TextField(
                    value = input2,
                    onValueChange = { input2 = it },
                    label = { Text(label2) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester2),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onConfirm(input1, input2)
                            onDismissRequest()
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(input1, input2)
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

    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }
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
    errorMessage: String?,
) {
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }
    var input1 by remember { mutableStateOf(initialInput) }
    var input2 by remember { mutableStateOf(initialInput) }
    var input3 by remember { mutableStateOf(initialInput) }
    var input4 by remember { mutableStateOf(initialInput) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = input1,
                    onValueChange = { input1 = it },
                    label = { Text(label1) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester1),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusRequester2.requestFocus()
                        }
                    )
                )
                TextField(
                    value = input2,
                    onValueChange = { input2 = it },
                    label = { Text(label2) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester2),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusRequester3.requestFocus()
                        }
                    )
                )
                TextField(
                    value = input3,
                    onValueChange = { input3 = it },
                    label = { Text(label3) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester3),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusRequester4.requestFocus()
                        }
                    )
                )
                TextField(
                    value = input4,
                    onValueChange = { input4 = it },
                    label = { Text(label4) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester4),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onConfirm(input1, input2, input3, input4)
                        }
                    )
                )
                AnimatedVisibility(errorMessage != null) {
                    SelectionContainer {
                        Text(
                            errorMessage!!,
                            modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(input1, input2, input3, input4)
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

    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }
}