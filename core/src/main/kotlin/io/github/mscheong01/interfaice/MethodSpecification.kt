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
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

data class MethodSpecification(
    val suspend: Boolean,
    val name: String,
    val parameters: List<ParameterSpecification>,
    val description: String? = null,
    val returnType: TypeSpecification<*>
) {
    companion object {
        fun from(method: Method, args: Array<out Any>?): MethodSpecification {
            val isSuspend = method.isSuspendingFunction()
            val parameters: List<ParameterSpecification>
            val returnType: TypeSpecification<*>
            if (args == null) {
                returnType = TypeSpecification(
                    klazz = method.returnType.kotlin,
                    javaType = method.genericReturnType
                )
                parameters = listOf()
            } else if (isSuspend) {
                val continuation = method.genericParameterTypes.get(method.genericParameterTypes.size - 1)
                val type = ((continuation as ParameterizedType).actualTypeArguments.first() as WildcardType).lowerBounds.first()
                val klazz = if (type is ParameterizedType) {
                    (type.rawType as Class<*>).kotlin
                } else {
                    (type as Class<*>).kotlin
                }
                returnType = TypeSpecification(
                    klazz = klazz,
                    javaType = type
                )
                parameters = method.parameters.dropLast(1).mapIndexed { index, parameter ->
                    ParameterSpecification(
                        name = parameter.name,
                        type = TypeSpecification(
                            klazz = parameter.type.kotlin,
                            javaType = parameter.parameterizedType
                        ),
                        value = args[index]
                    )
                }
            } else {
                parameters = method.parameters.mapIndexed { index, parameter ->
                    ParameterSpecification(
                        name = parameter.name,
                        type = TypeSpecification(
                            klazz = parameter.type.kotlin,
                            javaType = parameter.parameterizedType
                        ),
                        value = args[index]
                    )
                }
                returnType = TypeSpecification(
                    klazz = method.returnType.kotlin,
                    javaType = method.genericReturnType
                )
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
    val type: TypeSpecification<*>,
    val `value`: Any? = null
)
open class TypeSpecification<T : Any>(
    val klazz: KClass<T>,
    val javaType: Type
) {
    val typeArguments: List<TypeSpecification<*>>
        get() = (javaType as ParameterizedType).actualTypeArguments.map {
            val klazz = if (it is ParameterizedType) {
                (it.rawType as Class<*>).kotlin
            } else {
                (it as Class<*>).kotlin
            }
            TypeSpecification(
                klazz = klazz,
                javaType = it
            )
        }

    val isArray: Boolean
        get() {
            return Array<Any>::class.java.isAssignableFrom(this.klazz.java)
        }

    val arrayTypeArgument: TypeSpecification<*>
        get() {
            if (!isArray) {
                throw IllegalStateException("Type: ${this.javaType.typeName} is not an array")
            }
            val entryType = (javaType as Class<*>).componentType
            val klazz = if (entryType is ParameterizedType) {
                (entryType.rawType as Class<*>).kotlin
            } else {
                (entryType as Class<*>).kotlin
            }
            return TypeSpecification(
                klazz = klazz,
                javaType = entryType
            )
        }

    val isReactiveWrapper: Boolean
        get() {
            val qualifiedName = klazz.qualifiedName
            return qualifiedName != null &&
                (
                    qualifiedName.startsWith("reactor.core.publisher") ||
                        qualifiedName.startsWith("kotlinx.coroutines.flow")
                    )
        }

    companion object {
        fun <T : Any> from(obj: T): TypeSpecification<*> {
            return TypeSpecification(
                klazz = obj::class,
                javaType = obj::class.java
            )
        }
    }
}

fun Method.returnTypeSpec(): TypeSpecification<*> {
    return TypeSpecification(
        klazz = this.returnType.kotlin,
        javaType = this.genericReturnType
    )
}
