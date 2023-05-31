// Copyright 2023 Minsoo Cheong
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.github.mscheong01.interfaice.openai

class MockOpenAiApiAdapter : OpenAiApiAdapter {
    override fun setApiKey(apiKey: String) {
        // do nothing
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        println(request)
        return ChatResponse(
            id = "cmpl-3KJYz4J5jzv5J",
            created = 1627896549,
            model = "davinci:2020-05-03",
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(
                        role = ChatMessageRole.ASSISTANT,
                        content = "Hello, I am a chatbot. I am here to help you.",
                    ),
                    finishReason = "stop",
                )
            ),
            `object` = "text_completion",
            usage = ChatUsage(
                promptTokens = 7,
                completionTokens = 7,
                totalTokens = 1,
            ),
        )
    }
}
