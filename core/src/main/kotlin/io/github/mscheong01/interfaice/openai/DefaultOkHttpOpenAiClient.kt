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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultOkHttpOpenAiClient : OpenAiApiAdapter {
    private lateinit var apiKey: String
    private val client = OkHttpClient()
    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val requestBody = mapper.writeValueAsString(request)
        println(requestBody)
        val httpRequest = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
            .header("Authorization", "Bearer $apiKey")
            .build()
        return withContext(Dispatchers.IO) {
            with (client.newCall(httpRequest).execute()) {
                if (this.isSuccessful) {
                    return@withContext mapper.readValue(this.body().string())
                } else {
                    throw Exception("Error: ${this.code()} ${this.message()}")
                }
            }
        }
    }

    companion object {
        val mapper = jacksonObjectMapper()
    }
}
