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
package io.github.mscheong01.interfaice

import io.github.mscheong01.interfaice.util.isSuspendingFunction
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

data class MethodSpecification(
    val suspend: Boolean,
    val name: String,
    val parameters: List<ParameterSpecification>,
    val description: String? = null,
    val returnType: KClass<out Any>
) {
    companion object {
        fun from(method: Method, args: Array<out Any>?): MethodSpecification {
            val isSuspend = method.isSuspendingFunction()
            val parameters: List<ParameterSpecification>
            val returnType: KClass<out Any>
            if (args == null) {
                returnType = method.returnType.kotlin
                parameters = listOf()
            } else if (isSuspend) {
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
    }
}
data class ParameterSpecification(
    val name: String,
    val type: KClass<out Any>,
    val `value`: Any? = null
)
