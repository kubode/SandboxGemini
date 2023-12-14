package com.example.sandboxgemini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sandboxgemini.ui.theme.SandboxGeminiTheme
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SandboxGeminiTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                }
            }
        }
    }
}

@Composable
private fun GeminiScreen() {
    Column {
        var inputText: String by remember { mutableStateOf("") }
        var messages: ImmutableList<Message> by remember { mutableStateOf(persistentListOf()) }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            items(messages, key = { it.uuid }) {

            }
        }
        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { /*TODO*/ }) {
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

}

@Composable
private fun UseMessage(
    message: Message.User,
    modifier: Modifier = Modifier,
) {

}

@Immutable
private sealed class Message {
    abstract val uuid: String
    abstract val text: String

    @Immutable
    data class User(override val text: String, override val uuid: String) : Message()

    @Immutable
    sealed class Gemini : Message() {
        @Immutable
        data class InProgress(override val text: String, override val uuid: String) : Gemini()

        @Immutable
        data class Success(override val text: String, override val uuid: String) : Gemini()

        @Immutable
        data class Failure(override val text: String, override val uuid: String) : Gemini()
    }
}

private suspend fun gemini() {
    val generativeModel = GenerativeModel(
        // Use a model that's applicable for your use case (see "Implement basic use cases" below)
        // https://ai.google.dev/models/gemini
        modelName = "gemini-pro",
        // Access your API key as a Build Configuration variable (see "Set up your API key" above)
        apiKey = BuildConfig.apiKey,
    )
    val prompt = "Write a story about a magic backpack."
    val response = generativeModel.generateContent(prompt)
    print(response.text)
}
