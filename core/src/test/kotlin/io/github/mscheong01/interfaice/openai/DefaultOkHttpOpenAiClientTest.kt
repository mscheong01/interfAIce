package io.github.mscheong01.interfaice.openai

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

// @Disabled
internal class DefaultOkHttpOpenAiClientTest {
    @Test
    fun testChat(): Unit = runBlocking {
        val client = DefaultOkHttpOpenAiClient()
        client.setApiKey(System.getenv("OPENAI_API_KEY"))
        val response = client.chat(
            ChatRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(
                        ChatMessageRole.USER,
                        "What is the meaning of life?"
                    )
                )
            )
        )
        println(response)
    }
}
