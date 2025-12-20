package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
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

enum class Page {
    LOGIN,
    REGISTER,
    CHANGE_PASS,
    SIMULATOR
}

@Composable
fun App() {
    MaterialTheme {
        var apiType by remember { mutableStateOf("SkyNoNs") }
        var api by remember { mutableStateOf<SkynonsClientApi>(SkynonsClientApiImpl("")) }
        val scope = rememberCoroutineScope()

        var clientLogin by remember { mutableStateOf("") }
        var hash by remember { mutableStateOf("") }
        var authorizationStatus by remember { mutableStateOf(false) }
        var page by remember { mutableStateOf(Page.SIMULATOR) }

        window.onhashchange = { e ->
            if (e.newURL.contains('#')) {
                hash = e.newURL.substringAfter('#')
                page = when (hash) {
                    "AuthPage" -> Page.LOGIN
                    "RegisterPage" -> Page.REGISTER
                    "ChangePasswordPage" -> Page.CHANGE_PASS
                    else -> page
                }
                if (hash.startsWith("Simulator-")) {
                    page = Page.SIMULATOR
                }
            }
        }

        when (page) {
            Page.SIMULATOR -> {
                SimulatorPage(
                    api, clientLogin, authorizationStatus, hash, apiType,
                    onApiChanged = { newApi, type ->
                        api = newApi
                        apiType = type
                    },
                    onLogout = {
                        scope.launch(Dispatchers.Default) {
                            try {
                                api.logout()
                            } catch (_: Exception) {
                            } finally {
                                authorizationStatus = false
                            }
                        }
                    },
                    onLogin = {
                        page = Page.LOGIN
                    },
                    onChangePassword = {
                        page = Page.CHANGE_PASS
                    }
                )
            }

            Page.LOGIN -> {
                AuthorizationPage(
                    api,
                    onAuthorized = { login ->
                        clientLogin = login
                        authorizationStatus = true
                        page = Page.SIMULATOR
                    },
                    onTryRegister = {
                        page = Page.REGISTER
                    },
                    onReturn = {
                        page = Page.SIMULATOR
                    }
                )
            }

            Page.REGISTER -> {
                RegisterPage(
                    api,
                    onAuthorized = { login ->
                        clientLogin = login
                        authorizationStatus = true
                        page = Page.SIMULATOR
                    },
                    onTryLogin = {
                        page = Page.LOGIN
                    },
                    onReturn = {
                        page = Page.SIMULATOR
                    }
                )
            }

            Page.CHANGE_PASS -> {
                ChangePasswordPage(
                    api,
                    clientLogin,
                    onReturn = {
                        page = Page.SIMULATOR
                    }
                )
            }
        }
    }
}

@Composable
fun AuthorizationPage(
    api: SkynonsClientApi,
    onAuthorized: (String) -> Unit,
    onTryRegister: () -> Unit,
    onReturn: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        window.location.hash = "AuthPage"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(3))
                .background(
                    color = Color(0.9f, 0.9f, 0.9f, 0.9f),
                    shape = RoundedCornerShape(3)
                )
                .padding(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Вернуться", modifier = Modifier.padding(5.dp).clickable(onClick = onReturn))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var errorMessage by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(5))
                    .background(
                        color = Color(0.95f, 0.95f, 0.95f, 1f),
                        shape = RoundedCornerShape(5)
                    )
                    .width((window.innerWidth * 0.2).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var loginInput by remember { mutableStateOf("") }
                    var passwordInput by remember { mutableStateOf("") }
                    val focusRequester1 = remember { FocusRequester() }
                    val focusRequester2 = remember { FocusRequester() }

                    Text("Вход в аккаунт")

                    TextField(
                        value = loginInput,
                        onValueChange = { loginInput = it },
                        label = { Text("Имя аккаунта") },
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
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester2),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequester1.requestFocus()
                            }
                        )
                    )

                    LaunchedEffect(Unit) {
                        focusRequester1.requestFocus()
                    }

                    Button(
                        onClick = {
                            if (loginInput != "" && passwordInput != "") {
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        val response = api.authenticate(loginInput, passwordInput)
                                        val result = response.resultOrNull()
                                        if (result != null) {
                                            errorMessage = ""
                                            onAuthorized(result.login)
                                        } else {
                                            val error = response.errorOrNull()
                                            errorMessage = error?.message ?: "Unexpected error"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Unexpected error: ${e.message}"
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.ADD
                    ) {
                        Text("Войти")
                    }

                    Row {
                        Text("Еще нет аккаунта? ")
                        Text(
                            buildAnnotatedString {
                                append("Зарегистрируйтесь")
                                addStyle(
                                    style = SpanStyle(
                                        color = Color(0xff64B5F6),
                                        textDecoration = TextDecoration.Underline
                                    ), start = 0, end = 17
                                )
                            },
                            modifier = Modifier.clickable(onClick = onTryRegister)
                        )
                    }
                }
            }

            AnimatedVisibility(errorMessage != "") {
                SelectionContainer {
                    Text(
                        " $errorMessage ",
                        modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterPage(
    api: SkynonsClientApi,
    onAuthorized: (String) -> Unit,
    onTryLogin: () -> Unit,
    onReturn: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        window.location.hash = "RegisterPage"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(3))
                .background(
                    color = Color(0.9f, 0.9f, 0.9f, 0.9f),
                    shape = RoundedCornerShape(3)
                )
                .padding(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Вернуться", modifier = Modifier.padding(5.dp).clickable(onClick = onReturn))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            var errorMessage by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(5))
                    .background(
                        color = Color(0.95f, 0.95f, 0.95f, 1f),
                        shape = RoundedCornerShape(5)
                    )
                    .width((window.innerWidth * 0.2).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var loginInput by remember { mutableStateOf("") }
                    var passwordInput by remember { mutableStateOf("") }
                    var passwordRepeatInput by remember { mutableStateOf("") }
                    val focusRequester1 = remember { FocusRequester() }
                    val focusRequester2 = remember { FocusRequester() }
                    val focusRequester3 = remember { FocusRequester() }

                    Text("Регистрация")

                    TextField(
                        value = loginInput,
                        onValueChange = { loginInput = it },
                        label = { Text("Имя аккаунта") },
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
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester2),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequester3.requestFocus()
                            }
                        )
                    )
                    TextField(
                        value = passwordRepeatInput,
                        onValueChange = { passwordRepeatInput = it },
                        label = { Text("Повтор пароля") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester3),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequester1.requestFocus()
                            }
                        )
                    )

                    LaunchedEffect(Unit) {
                        focusRequester1.requestFocus()
                    }

                    Button(
                        onClick = {
                            if (loginInput != "" && passwordInput != "") {
                                if (passwordInput != passwordRepeatInput) {
                                    errorMessage = "Пароли должны совпадать"
                                } else {
                                    scope.launch(Dispatchers.Default) {
                                        try {
                                            val response = api.register(loginInput, passwordInput)
                                            val result = response.resultOrNull()
                                            if (result != null) {
                                                errorMessage = ""
                                                onAuthorized(result.login)
                                            } else {
                                                val error = response.errorOrNull()
                                                errorMessage = error?.message ?: "Unexpected error"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Unexpected error: ${e.message}"
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.ADD
                    ) {
                        Text("Создать аккаунт")
                    }

                    Row {
                        Text("Уже есть аккаунт? ")
                        Text(
                            buildAnnotatedString {
                                append("Войти")
                                addStyle(
                                    style = SpanStyle(
                                        color = Color(0xff64B5F6),
                                        textDecoration = TextDecoration.Underline
                                    ), start = 0, end = 5
                                )
                            },
                            modifier = Modifier.clickable(onClick = onTryLogin)
                        )
                    }
                }
            }

            AnimatedVisibility(errorMessage != "") {
                SelectionContainer {
                    Text(
                        " $errorMessage ",
                        modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                    )
                }
            }
        }
    }
}

@Composable
fun ChangePasswordPage(
    api: SkynonsClientApi,
    clientLogin: String,
    onReturn: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        window.location.hash = "ChangePasswordPage"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(3))
                .background(
                    color = Color(0.9f, 0.9f, 0.9f, 0.9f),
                    shape = RoundedCornerShape(3)
                )
                .padding(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Вернуться", modifier = Modifier.padding(5.dp).clickable(onClick = onReturn))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            var errorMessage by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(5))
                    .background(
                        color = Color(0.95f, 0.95f, 0.95f, 1f),
                        shape = RoundedCornerShape(5)
                    )
                    .width((window.innerWidth * 0.2).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var oldpassInput by remember { mutableStateOf("") }
                    var passwordInput by remember { mutableStateOf("") }
                    var passwordRepeatInput by remember { mutableStateOf("") }
                    val focusRequester1 = remember { FocusRequester() }
                    val focusRequester2 = remember { FocusRequester() }
                    val focusRequester3 = remember { FocusRequester() }

                    Text("Смена пароля")

                    TextField(
                        value = oldpassInput,
                        onValueChange = { oldpassInput = it },
                        label = { Text("Старый пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester1),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequester2.requestFocus()
                            }
                        )
                    )
                    TextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Новый пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester2),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequester3.requestFocus()
                            }
                        )
                    )
                    TextField(
                        value = passwordRepeatInput,
                        onValueChange = { passwordRepeatInput = it },
                        label = { Text("Повтор пароля") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(2.dp).focusRequester(focusRequester3),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusRequester1.requestFocus()
                            }
                        )
                    )

                    LaunchedEffect(Unit) {
                        focusRequester1.requestFocus()
                    }

                    Button(
                        onClick = {
                            if (oldpassInput != "" && passwordInput != "") {
                                if (passwordInput != passwordRepeatInput) {
                                    errorMessage = "Пароли должны совпадать"
                                } else {
                                    scope.launch(Dispatchers.Default) {
                                        try {
                                            val response = api.updatePassword(oldpassInput, passwordInput)
                                            val result = response.resultOrNull()
                                            if (result != null) {
                                                errorMessage = ""
                                                onReturn()
                                            } else {
                                                val error = response.errorOrNull()
                                                errorMessage = error?.message ?: "Unexpected error"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Unexpected error: ${e.message}"
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.ADD
                    ) {
                        Text("Сменить пароль")
                    }

                    Text("Аккаунт: $clientLogin")
                }
            }

            AnimatedVisibility(errorMessage != "") {
                SelectionContainer {
                    Text(
                        " $errorMessage ",
                        modifier = Modifier.background(Color(1f, 0.5f, 0.5f), RoundedCornerShape(5))
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatorPage(
    api: SkynonsClientApi,
    clientLogin: String,
    authorizationStatus: Boolean,
    hash: String,
    apiType: String,
    onApiChanged: (SkynonsClientApi, String) -> Unit,
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onChangePassword: () -> Unit,
) {
    val localContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()

    var simulationId by remember { mutableStateOf<String?>(null) }

    var connectionError by remember { mutableStateOf<String?>(null) }
    var simulationStateOverride by remember { mutableStateOf<SimulationState?>(null) }

    val simulationsHistory = remember { mutableStateListOf<HistoryEntry>() }

    LaunchedEffect(authorizationStatus, clientLogin) {
        if (authorizationStatus) {
            try {
                println("Requesting history...")
                val resp = api.listHistory()
                val list = resp.resultOrNull()
                println("Got $list")
                if (list != null) {
                    simulationsHistory.addAll(list.entries)
                }
                println("Now 1 $simulationsHistory")
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(api, hash) {
        runCatching {
            if (hash.startsWith("Simulator-") && hash.substringAfter("Simulator-") != "") {
                try {
                    simulationId = null
                    val resp = api.state(hash.substringAfter("Simulator-"))
                    simulationStateOverride = resp.resultOrNull()
                    if (simulationStateOverride == null) {
                        connectionError = resp.errorOrNull()?.message ?: "Unknown Exception"
                        simulationId = null
                    } else {
                        simulationId = hash.substringAfter("Simulator-")
                        connectionError = null
                    }
                } catch (e: Exception) {
                    connectionError = e.message
                    simulationId = null
                }
            } else {
                val resp = api.createSimulation()
                val result = resp.resultOrNull()
                if (result == null) {
                    connectionError =
                        "Не удалось подключиться к серверу: ${resp.errorOrNull()?.message ?: "Неопознанная ошибка"}"
                    simulationId = null
                } else {
                    window.location.hash = "Simulator-$result"
                    simulationId = result
                }
            }
        }.onFailure {
            connectionError =
                "Не удалось подключиться к серверу: ${it.message ?: "Неопознанная ошибка"}"
            simulationId = null
        }
    }
    SideEffect {
        document.onkeydown = handler@{ event ->
            if (!event.ctrlKey) return@handler
            when (event.key) {
                "l", "L" -> {
                    simulationId = null
                    onApiChanged(SkynonsClientApiMock(), "Mock")
                }

                "e", "E" -> {
                    simulationId = null
                    onApiChanged(SkynonsClientApiImpl(""), "SkyNoNs")
                }

                "d", "D" -> {
                    simulationId = null
                    onApiChanged(SkynonsClientApiImpl(), "Local")
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
            simulationId != null -> GraphRedactor(localContext, scope, api, simulationId, simulationStateOverride)
            connectionError != null -> Text("Не удалось подключиться к серверу $connectionError")
            else -> {
                Text("Подключение к серверу...")
                CircularProgressIndicator()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            var changePassButtonVisible by remember { mutableStateOf(false) }
            var historyVisible by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(3))
                    .background(
                        color = Color(0.9f, 0.9f, 0.9f, 0.9f),
                        shape = RoundedCornerShape(3)
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
//            Text("$apiType | $hash")

                if (authorizationStatus) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row {
                            Column {
                                Text(
                                    "Настройки",
                                    modifier = Modifier.padding(5.dp)
                                        .clickable(onClick = {
                                            changePassButtonVisible = !changePassButtonVisible
                                        })
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row {
                        if (authorizationStatus) {
                            Text("Аккаунт: $clientLogin. ", modifier = Modifier.padding(5.dp))
                            Text(
                                buildAnnotatedString {
                                    append("Выйти")
                                    addStyle(
                                        style = SpanStyle(
                                            color = Color(0xff64B5F6),
                                            textDecoration = TextDecoration.Underline
                                        ), start = 0, end = 5
                                    )
                                },
                                modifier = Modifier.padding(5.dp).clickable(onClick = onLogout)
                            )
                        } else {
                            Text("Вы не авторизованы. ", modifier = Modifier.padding(5.dp).clickable(onClick = onLogin))
                            Text(
                                buildAnnotatedString {
                                    append("Войти")
                                    addStyle(
                                        style = SpanStyle(
                                            color = Color(0xff64B5F6),
                                            textDecoration = TextDecoration.Underline
                                        ), start = 0, end = 5
                                    )
                                },
                                modifier = Modifier.padding(5.dp).clickable(onClick = onLogin)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(changePassButtonVisible) {
                Column(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(3))
                        .background(
                            color = Color(0.95f, 0.95f, 0.95f, 1f),
                            shape = RoundedCornerShape(3)
                        )
                        .padding(5.dp)
                ) {
                    Button(
                        onClick = onChangePassword,
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.ADD
                    ) {
                        Text("Сменить пароль")
                    }
                    Button(
                        onClick = { historyVisible = !historyVisible },
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.ADD
                    ) {
                        Text("История симуляций")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    val resp = api.createSimulation()
                                    val result = resp.resultOrNull()
                                    if (result != null) {
                                        window.location.hash = "Simulator-$result"
                                        simulationId = result
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10),
                        colors = ButtonColor.ADD
                    ) {
                        Text("Новая симуляция")
                    }
                }
            }

            AnimatedVisibility(historyVisible && changePassButtonVisible) {
                Column(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(3))
                        .background(
                            color = Color(0.95f, 0.95f, 0.95f, 1f),
                            shape = RoundedCornerShape(3)
                        )
                        .padding(5.dp)
                ) {
                    simulationsHistory.forEach { entry ->
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        val resp = api.createSimulationWithConfig(entry.config)
                                        val result = resp.resultOrNull()
                                        if (result != null) {
                                            window.location.hash = "Simulator-$result"
                                            simulationId = result
                                            val resp = api.state(result)
                                            simulationStateOverride = resp.resultOrNull()
                                            if (simulationStateOverride == null) {
                                                connectionError = resp.errorOrNull()?.message ?: "Unknown Exception"
                                                simulationId = null
                                            }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10),
                            colors = ButtonColor.ADD
                        ) {
                            Text("Симуляция №${entry.id}")
                        }
                    }
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
    simulationId: SimulationId,
    simulationStateOverride: SimulationState?,
) {
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

    LaunchedEffect(simulationStateOverride, simulationId) {
        if (simulationStateOverride != null) {
            state.loadConfig(simulationStateOverride, simulationId)
            if (simulationStateOverride.result != null) {
                ImageRequest.Builder(localContext).data(simulationStateOverride.result!!.rtt.toByteArray()).build()
                    .also { rttSvg = it }
                cwndSvg =
                    ImageRequest.Builder(localContext).data(simulationStateOverride.result!!.cwnd.toByteArray()).build()
                rateSvg =
                    ImageRequest.Builder(localContext).data(simulationStateOverride.result!!.rate.toByteArray()).build()
                packetReorderingSvg =
                    ImageRequest.Builder(localContext)
                        .data(simulationStateOverride.result!!.packetReordering.toByteArray())
                        .build()
            }
        }
    }

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
