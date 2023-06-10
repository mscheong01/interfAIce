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

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

class OpenAiApiClient : OpenAiApiAdapter {
    private lateinit var properties: OpenAiProperties
    private lateinit var webClient: WebClient

    override fun getProperties(): OpenAiProperties {
        return properties
    }

    override fun setProperties(properties: OpenAiProperties) {
        this.properties = properties
        this.webClient = WebClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.apiKey}")
            .build()
    }

    override fun chat(request: ChatRequest): Mono<ChatResponse> {
        return webClient.post()
            .uri("/v1/chat/completions")
            .body(BodyInserters.fromValue(request))
            .retrieve()
            .bodyToMono<ChatResponse>()
    }
}
