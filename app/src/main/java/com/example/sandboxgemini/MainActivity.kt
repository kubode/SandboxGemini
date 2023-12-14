package com.example.sandboxgemini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sandboxgemini.ui.theme.SandboxGeminiTheme
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setDecorFitsSystemWindows(false)
        super.onCreate(savedInstanceState)
        setContent {
            SandboxGeminiTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screenState by remember { mutableStateOf(GeminiScreenState()) }
                    val coroutineScope = rememberCoroutineScope()
                    GeminiScreen(
                        screenState = screenState,
                        onInputTextChange = {
                            screenState = screenState.copy(
                                inputText = it,
                            )
                        },
                        onSendClick = {
                            val prompt = screenState.inputText
                            screenState = screenState.copy(
                                inputText = "",
                                messages = screenState.messages.toMutableList()
                                    .apply {
                                        val newMessage = Message.User(
                                            uuid = generateRandomUuid(),
                                            text = screenState.inputText,
                                        )
                                        add(newMessage)
                                    }
                                    .toPersistentList()
                            )
                            coroutineScope.launch {
                                try {
                                    generativeModel
                                        .generateContentStream(prompt = prompt)
                                        .collect { response ->
                                            val lastMessage = screenState.messages.lastOrNull()
                                            if (lastMessage is Message.Gemini.InProgress) {
                                                screenState = screenState.copy(
                                                    messages = screenState.messages.toMutableList()
                                                        .apply {
                                                            removeLast()
                                                            val newMessage = lastMessage.copy(
                                                                text = lastMessage.text + response.text,
                                                            )
                                                            add(newMessage)
                                                        }
                                                        .toPersistentList()
                                                )
                                            } else {
                                                screenState = screenState.copy(
                                                    messages = screenState.messages.toMutableList()
                                                        .apply {
                                                            val newMessage =
                                                                Message.Gemini.InProgress(
                                                                    uuid = generateRandomUuid(),
                                                                    text = response.text.orEmpty(),
                                                                )
                                                            add(newMessage)
                                                        }
                                                        .toPersistentList()
                                                )
                                            }
                                        }
                                    screenState = screenState.copy(
                                        messages = screenState.messages.toMutableList()
                                            .apply {
                                                val lastMessage = screenState.messages.lastOrNull()
                                                if (lastMessage is Message.Gemini.InProgress) {
                                                    removeLast()
                                                    val newMessage = Message.Gemini.Success(
                                                        uuid = lastMessage.uuid,
                                                        text = lastMessage.text,
                                                    )
                                                    add(newMessage)
                                                }
                                            }
                                            .toPersistentList()
                                    )
                                } catch (e: Exception) {
                                    if (e is CancellationException) {
                                        throw e
                                    }
                                    screenState = screenState.copy(
                                        messages = screenState.messages.toMutableList()
                                            .apply {
                                                val lastMessage = screenState.messages.lastOrNull()
                                                if (lastMessage is Message.Gemini.InProgress) {
                                                    removeLast()
                                                    val newMessage = Message.Gemini.Failure(
                                                        uuid = lastMessage.uuid,
                                                        text = lastMessage.text,
                                                    )
                                                    add(newMessage)
                                                }
                                            }
                                            .toPersistentList()
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiScreen(
    screenState: GeminiScreenState,
    onInputTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val lastMessage = screenState.messages.lastOrNull()
    LaunchedEffect(lastMessage) {
        lastMessage ?: return@LaunchedEffect
        lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(screenState.messages, key = { it.uuid }) { message ->
                when (message) {
                    is Message.Gemini -> GeminiMessage(message = message)
                    is Message.User -> UserMessage(message = message)
                }
            }
        }
        TextField(
            value = screenState.inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = onSendClick,
                    enabled = screenState.isSendButtonEnabled,
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                }
            }
        )
    }
}

@Composable
private fun GeminiMessage(
    message: Message.Gemini,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 40.dp),
    ) {
        Surface(
            modifier = modifier
                .align(Alignment.CenterStart),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = message.text,
                modifier = Modifier
                    .padding(all = 8.dp),
            )
        }
    }
}

@Composable
private fun UserMessage(
    message: Message.User,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp)
    ) {
        Surface(
            modifier = modifier
                .align(Alignment.CenterEnd),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = message.text,
                modifier = Modifier
                    .padding(all = 8.dp),
            )
        }
    }
}

@Immutable
private data class GeminiScreenState(
    val inputText: String = "",
    val messages: ImmutableList<Message> = persistentListOf(),
) {
    val isSendButtonEnabled: Boolean
        get() = inputText.isNotBlank() && messages.lastOrNull() !is Message.Gemini.InProgress
}

@Immutable
private sealed class Message {
    abstract val uuid: String
    abstract val text: String

    @Immutable
    data class User(override val uuid: String, override val text: String) : Message()

    @Immutable
    sealed class Gemini : Message() {
        @Immutable
        data class InProgress(override val uuid: String, override val text: String) : Gemini()

        @Immutable
        data class Success(override val uuid: String, override val text: String) : Gemini()

        @Immutable
        data class Failure(override val uuid: String, override val text: String) : Gemini()
    }
}

private val generativeModel = GenerativeModel(
    // Use a model that's applicable for your use case (see "Implement basic use cases" below)
    // https://ai.google.dev/models/gemini
    modelName = "gemini-pro",
    // Access your API key as a Build Configuration variable (see "Set up your API key" above)
    apiKey = BuildConfig.apiKey,
)

private fun generateRandomUuid(): String {
    return UUID.randomUUID().toString()
}

@Composable
@Preview
private fun GeminiScreenPreview() {
    SandboxGeminiTheme {
        GeminiScreen(
            screenState = GeminiScreenState(
                inputText = "Hello, world!",
                messages = persistentListOf(
                    Message.User(
                        uuid = generateRandomUuid(),
                        text = "Hello, world!",
                    ),
                    Message.Gemini.InProgress(
                        uuid = generateRandomUuid(),
                        text = "Hello, world!",
                    ),
                    Message.User(
                        uuid = generateRandomUuid(),
                        text = "Hello, world!",
                    ),
                    Message.Gemini.Failure(
                        uuid = generateRandomUuid(),
                        text = "Hello, world!",
                    ),
                    Message.User(
                        uuid = generateRandomUuid(),
                        text = "Hello, world!",
                    ),
                    Message.Gemini.Success(
                        uuid = generateRandomUuid(),
                        text = "Hello, world!",
                    ),
                )
            ),
            onInputTextChange = {},
            onSendClick = {},
        )
    }
}
