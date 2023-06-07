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

import io.github.mscheong01.interfaice.MethodSpecification
import io.github.mscheong01.interfaice.TextObjectTranscoder
import io.github.mscheong01.interfaice.TranscodingRules
import io.github.mscheong01.interfaice.util.isSuspendingFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OpenAiInvocationHandler(
    private val interfaceName: String,
    private val openAiApiAdapter: OpenAiApiAdapter
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val openAiChatAnnotation: OpenAiChat? = method.getAnnotation(OpenAiChat::class.java)
        val model = openAiChatAnnotation?.model ?: OpenAiChat.DEFAULT_MODEL
        val description = openAiChatAnnotation?.description

        val specification = MethodSpecification.from(method, args)
        val returnTypeEncodePrompt = if (specification.returnType.isReactiveWrapper) {
            when (specification.returnType.klazz) {
                Mono::class -> TranscodingRules.match(specification.returnType.typeArguments.first()).encodeDescription
                Flux::class -> TranscodingRules.ListRule(specification.returnType.typeArguments.first()).encodeDescription
                Flow::class -> TranscodingRules.ListRule(specification.returnType.typeArguments.first()).encodeDescription
                else -> throw IllegalArgumentException("Unsupported reactive type: ${specification.returnType.klazz}")
            }
        } else {
            return TranscodingRules.match(specification.returnType).encodeDescription
        }

        val responseMono = openAiApiAdapter.chat(
            ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(
                        ChatMessageRole.SYSTEM,
                        """
                            You will be given a method spec of a method defined by the user as a member of interface '%s'.
                            By Carefully following the method spec, respond as the method would.
                            When responding, follow the given format without any additional text. Keep in mind that your response will be decoded and provided as the method response.
                            response format: %s
                        """.format(
                            interfaceName,
                            returnTypeEncodePrompt
                        ).trimIndent()
                    ),
                    ChatMessage(
                        ChatMessageRole.USER,
                        """
                            method spec:
                            name = %s
                            parameters = {
                                %s
                            }
                            %s
                            
                            Note that some parameters may be NULL.
                            Again, make sure to follow the provided response format without any additional text.
                        """.format(
                            specification.name,
                            specification.parameters.joinToString { "${it.name} = ${transcoder.encode(it.value)}, " },
                            description.takeUnless { it.isNullOrEmpty() }?.let { "description = $it" } ?: ""
                        ).trimIndent()
                    )
                )
            )
        ).let { Mono.from(it) }.map { it.choices.first().message.content }

        // If it's a suspend function, use Kotlin coroutines to call the client in a non-blocking way
        return if (args != null && args.isNotEmpty() && method.isSuspendingFunction()) {
            val continuation = args.get(args.size - 1) as Continuation<Any>
            val job = CoroutineScope(continuation.context).async {
                responseMono.map { transcoder.decode(it, specification.returnType) }.awaitSingle()
            }
            job.invokeOnCompletion {
                if (it != null) {
                    continuation.resumeWithException(it)
                } else {
                    continuation.resume(job.getCompleted())
                }
            }
            return kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
        } else if (specification.returnType.isReactiveWrapper) {
            when (specification.returnType.klazz) {
                Mono::class -> {
                    responseMono.map { transcoder.decode(it, specification.returnType.typeArguments.first()) }
                }
                Flux::class -> {
                    responseMono.flatMapIterable { TranscodingRules.ListRule(specification.returnType.typeArguments.first()).decoder(it) }
                }
                Flow::class -> {
                    responseMono.flatMapIterable { TranscodingRules.ListRule(specification.returnType.typeArguments.first()).decoder(it) }.asFlow()
                }
                else -> throw IllegalStateException("Unsupported reactive wrapper: ${specification.returnType.klazz}")
            }
        } else {
            responseMono.block()?.let { transcoder.decode(it, specification.returnType) }
                ?: throw IllegalStateException("Response is null")
        }
    }

    companion object {
        val transcoder = TextObjectTranscoder()
    }
}
