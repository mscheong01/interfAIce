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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OpenAiInvocationHandler(val openAiApiAdapter: OpenAiApiAdapter) : InvocationHandler {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        val openAiChatAnnotation: OpenAiChat? = method.getAnnotation(OpenAiChat::class.java)

        if (openAiChatAnnotation != null) {
            val specification = MethodSpecification.from(method, args)
            val responseMono = mono {
                openAiApiAdapter.chat(
                    ChatRequest(
                        model = openAiChatAnnotation.model,
                        messages = listOf(
                            ChatMessage(
                                ChatMessageRole.SYSTEM,
                                """
                                    You will be given a method spec of a method defined by the user.
                                    By Carefully following the method spec, respond as the method would.
                                    When responding, follow the given format without any additional text. Keep in mind that your response will be decoded and provided as the method response:
                                    response format: %s
                                """.format(TranscodingRules.match(specification.returnType).encodeDescription).trimIndent()
                            ),
                            ChatMessage(
                                ChatMessageRole.USER,
                                """
                                    method spec:
                                    name = %s
                                    parameters = {
                                        %s
                                    }
                                    return type = %s
                                    %s
                                    
                                    Note that some parameters may be NULL.
                                    Again, make sure to follow the provided response format without any additional text.
                                """.format(
                                    specification.name,
                                    specification.parameters.joinToString { "${it.name} = ${transcoder.encode(it.value)}, " },
                                    specification.returnType.qualifiedName,
                                    openAiChatAnnotation.description.takeIf { it.isNotEmpty() }?.let { "description = $it" } ?: ""
                                ).trimIndent()
                            )
                        )
                    )
                ).choices.first().message.content.let { transcoder.decode(it, specification.returnType) }
            }

            // If it's a suspend function, use Kotlin coroutines to call the client in a non-blocking way
            return if (method.isSuspendingFunction()) {
                val continuation = args.get(args.size - 1) as Continuation<Any>
                val job = CoroutineScope(continuation.context).async {
                    responseMono.awaitSingle()
                }
                job.invokeOnCompletion {
                    if (it != null) {
                        continuation.resumeWithException(it)
                    } else {
                        continuation.resume(job.getCompleted())
                    }
                }
                return kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
            } else {
                responseMono.block()
            }
        }
        return null
    }

    companion object {
        val transcoder = TextObjectTranscoder()
    }
}
