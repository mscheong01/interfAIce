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
import io.github.mscheong01.interfaice.ParameterSpecification
import io.github.mscheong01.interfaice.TextObjectTranscoder
import io.github.mscheong01.interfaice.TranscodingRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

class OpenAiInvocationHandler(val openAiApiAdapter: OpenAiApiAdapter) : InvocationHandler {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
//        TODO("Not yet implemented")

        val openAiChatAnnotation: OpenAiChat? = method.getAnnotation(OpenAiChat::class.java)

        if (openAiChatAnnotation != null) {
            val specification = getSpecification(method, args)
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
                                    parameters = %s
                                    return type = %s
                                    
                                    again, make sure to follow the provided response format without any additional text
                                """.format(specification.name, specification.parameters.map { transcoder.encode(it.value!!) }.joinToString(", "), specification.returnType.qualifiedName).trimIndent()
                            )
                        )
                    )
                ).choices.first().message.content.let { transcoder.decode(it, specification.returnType) }
            }

            // If it's a suspend function, use Kotlin coroutines to call the client in a non-blocking way
            return if (isSuspendingFunction(method)) {
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

    fun getSpecification(method: Method, args: Array<out Any>): MethodSpecification {
        val isSuspend = isSuspendingFunction(method)
        val parameters: List<ParameterSpecification>
        val returnType: KClass<out Any>
        if (isSuspend) {
            val continuation = method.genericParameterTypes.get(method.genericParameterTypes.size - 1)
            returnType = ( // TODO: find alternative to this
                (
                    (continuation as ParameterizedType).actualTypeArguments.first() as WildcardType
                    ).lowerBounds.first() as Class<*>
                ).kotlin
            parameters = method.parameters.dropLast(1).mapIndexed { index, parameter ->
                ParameterSpecification(
                    name = parameter.name,
                    type = parameter.type.kotlin,
                    value = args[index]
                )
            }
        } else {
            parameters = method.parameters.mapIndexed { index, parameter ->
                ParameterSpecification(
                    name = parameter.name,
                    type = parameter.type.kotlin,
                    value = args[index]
                )
            }
            returnType = method.returnType.kotlin
        }
        return MethodSpecification(
            suspend = isSuspend,
            name = method.name,
            parameters = parameters,
            returnType = returnType
        )
    }

    companion object {
        val transcoder = TextObjectTranscoder()
    }
}
