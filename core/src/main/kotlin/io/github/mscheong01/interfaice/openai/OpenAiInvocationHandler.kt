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

import io.github.mscheong01.interfaice.TextObjectTranscoder
import io.github.mscheong01.interfaice.TranscodingRules
import kotlinx.coroutines.reactor.mono
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class OpenAiInvocationHandler(val openAiApiAdapter: OpenAiApiAdapter) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>): Any? {
//        TODO("Not yet implemented")

        val openAiChatAnnotation: OpenAiChat? = method.getAnnotation(OpenAiChat::class.java)

        if (openAiChatAnnotation != null) {

            val responseMono = mono {
                openAiApiAdapter.chat(
                    ChatRequest(
                        model = openAiChatAnnotation.model,
                        messages = listOf(
                            ChatMessage(
                                ChatMessageRole.SYSTEM,
                                """
                                    respond with the expected result of the given methods spec.
                                    Respond with only the required answer by follow the given format without any additional text:
                                    %s
                                """.format(TranscodingRules.match(method.returnType.kotlin).encodeDescription)
                            ),
                            ChatMessage(
                                ChatMessageRole.USER,
                                """
                                    method spec:
                                    name = %s
                                    parameters = %s
                                    return type = %s
                                """.format(method.name, args.map { transcoder.encode(it) }.joinToString(", "), method.returnType.name)
                            )
                        )
                    )
                ).choices.first().message!!.content.let { transcoder.decode(it, method.returnType.kotlin) }
            }

            // If it's a suspend function, use Kotlin coroutines to call the client in a non-blocking way
            return if (isSuspendingFunction(method)) {
                val context = args[0] as CoroutineContext
                val continuation = args.get(args.size - 1) as Continuation<Any>
                var response: Any? = null
                responseMono.subscribe {
                    response = it
                    continuation.resume(it)
                }
                response
            } else {
                responseMono.block()
            }!!
        }
        return null
    }

    fun isSuspendingFunction(method: Method): Boolean {
        val types = method.parameterTypes
        if (types.isNotEmpty() && "kotlin.coroutines.Continuation" == types[types.size - 1].name) {
            return true
        }
        return false
    }

    companion object {
        val transcoder = TextObjectTranscoder()
    }
}
