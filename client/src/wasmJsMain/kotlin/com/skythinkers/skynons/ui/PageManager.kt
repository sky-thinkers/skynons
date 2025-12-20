package com.skythinkers.skynons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.api.HistoryEntry
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.api.SimulationId
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.api.SkynonsClientApi
import com.skythinkers.skynons.api.SkynonsClientApiImpl
import com.skythinkers.skynons.api.SkynonsClientApiMock
import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.api.errorOrNull
import com.skythinkers.skynons.api.resultOrNull
import io.ktor.utils.io.core.toByteArray
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class Page {
    LOGIN,
    REGISTER,
    CHANGE_PASS,
    SIMULATOR
}

class PageManager(
    val scope: CoroutineScope,
) {
    var apiType by mutableStateOf("SkyNoNs")
    var api by mutableStateOf<SkynonsClientApi>(SkynonsClientApiImpl(""))

    var clientLogin by mutableStateOf("")
    var hash by mutableStateOf("")
    var authorizationStatus by mutableStateOf(false)
    var page by mutableStateOf(Page.SIMULATOR)

    fun setPage(p: Page) {
        page = p
    }

    @Composable
    fun showPage() {
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
            Page.SIMULATOR -> SimulatorPage()

            Page.LOGIN -> AuthorizationPage()

            Page.REGISTER -> RegisterPage()

            Page.CHANGE_PASS -> ChangePasswordPage()
        }
    }


    @Composable
    fun AuthorizationPage() {
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
                Text("Вернуться", modifier = Modifier.padding(5.dp).clickable(onClick = { setPage(Page.SIMULATOR) }))
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
                                                clientLogin = result.login
                                                authorizationStatus = true
                                                setPage(Page.SIMULATOR)
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
                                modifier = Modifier.clickable(onClick = { setPage(Page.REGISTER) })
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
    fun RegisterPage() {
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
                Text("Вернуться", modifier = Modifier.padding(5.dp).clickable(onClick = { setPage(Page.SIMULATOR) }))
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
                                                    clientLogin = result.login
                                                    authorizationStatus = true
                                                    setPage(Page.SIMULATOR)
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
                                modifier = Modifier.clickable(onClick = { setPage(Page.LOGIN) })
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
    fun ChangePasswordPage() {
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
                Text("Вернуться", modifier = Modifier.padding(5.dp).clickable(onClick = { setPage(Page.SIMULATOR) }))
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
                                                    setPage(Page.SIMULATOR)
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
    fun SimulatorPage() {
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
                        api = SkynonsClientApiMock()
                        apiType = "Mock"
                    }

                    "e", "E" -> {
                        simulationId = null
                        api = SkynonsClientApiImpl("")
                        apiType = "SkyNoNs"
                    }

                    "d", "D" -> {
                        simulationId = null
                        api = SkynonsClientApiImpl()
                        apiType = "Local"
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
                simulationId != null -> GraphRedactor(simulationId, simulationStateOverride)
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
                                    modifier = Modifier.padding(5.dp).clickable(onClick = {
                                        scope.launch(Dispatchers.Default) {
                                            try {
                                                api.logout()
                                            } catch (_: Exception) {
                                            } finally {
                                                authorizationStatus = false
                                            }
                                        }
                                    })
                                )
                            } else {
                                Text("Вы не авторизованы. ", modifier = Modifier.padding(5.dp).clickable(onClick = { setPage(Page.LOGIN) }))
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
                                    modifier = Modifier.padding(5.dp).clickable(onClick = { setPage(Page.LOGIN) })
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
                            onClick = { setPage(Page.CHANGE_PASS) },
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
        simulationId: SimulationId,
        simulationStateOverride: SimulationState?,
    ) {
        val localContext = LocalPlatformContext.current
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
}
