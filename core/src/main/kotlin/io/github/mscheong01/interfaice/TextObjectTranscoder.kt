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

import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class TextObjectTranscoder {
    inline fun <reified T : Any> encode(obj: T?): String {
        if (obj == null) return "NULL"
        val rule = TranscodingRules.match(TypeSpecification.from(obj))
        return rule.encoder(obj)
    }

    fun <T : Any> decode(str: String, type: TypeSpecification<T>): T {
        val rule = TranscodingRules.match(type)
        return rule.decoder(str)
    }

    fun <T : Any> getEncodePrompt(type: TypeSpecification<T>): String {
        return if (type.isReactiveWrapper) {
            when (type.klazz) {
                Mono::class -> TranscodingRules.match(type.typeArguments.first()).encodeDescription
                Flux::class -> TranscodingRules.ListRule(type.typeArguments.first()).encodeDescription
                Flow::class -> TranscodingRules.ListRule(type.typeArguments.first()).encodeDescription
                else -> throw IllegalArgumentException("Unsupported reactive type: ${type.klazz}")
            }
        } else {
            return TranscodingRules.match(type).encodeDescription
        }
    }
}
