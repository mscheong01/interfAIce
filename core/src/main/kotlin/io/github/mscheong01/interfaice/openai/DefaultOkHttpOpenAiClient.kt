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
import reactor.core.publisher.Mono
import java.io.IOException

class DefaultOkHttpOpenAiClient : OpenAiApiAdapter {
    private lateinit var properties: OpenAiProperties
    private val client = OkHttpClient()

    override fun getProperties(): OpenAiProperties {
        return properties
    }

    override fun setProperties(properties: OpenAiProperties) {
        this.properties = properties
    }

    override fun chat(request: ChatRequest): Mono<ChatResponse> {
        val requestBody = mapper.writeValueAsString(request)
        println(requestBody)
        val httpRequest = Request.Builder()
            .url("${properties.baseUrl}/v1/chat/completions")
            .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
            .header("Authorization", "Bearer ${properties.apiKey}")
            .build()
        return Mono.create { sink ->
            client.newCall(httpRequest).enqueue(object : com.squareup.okhttp.Callback {
                override fun onFailure(request: Request, e: java.io.IOException) {
                    sink.error(e)
                }

                override fun onResponse(response: com.squareup.okhttp.Response) {
                    try {
                        if (!response.isSuccessful) {
                            throw IOException("Error: ${response.code()} ${response.message()}")
                        } else {
                            sink.success(mapper.readValue(response.body().string()))
                        }
                    } catch (e: Exception) {
                        sink.error(e)
                    }
                }
            })
        }
    }

    companion object {
        val mapper = jacksonObjectMapper()
    }
}
