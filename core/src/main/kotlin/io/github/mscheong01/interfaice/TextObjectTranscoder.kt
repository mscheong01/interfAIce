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

import kotlin.reflect.full.isSubclassOf

class TextObjectTranscoder(
    val customRules: List<TranscodingRules.CustomRule<*>> = listOf()
) {
    inline fun <reified T : Any> encode(obj: T?): String {
        if (obj == null) return "NULL"
        val rule = match(TypeSpecification.from(obj))
        return rule.encode(this, obj)
    }
    fun <T : Any> encode(obj: T?, type: TypeSpecification<T>): String {
        if (obj == null) return "NULL"
        val rule = match(type)
        return rule.encode(this, obj)
    }

    fun <T : Any> decode(str: String, type: TypeSpecification<T>): T {
        val rule = match(type)
        return rule.decode(this, str)
    }

    fun <T : Any> encodeDescription(tyep: TypeSpecification<T>): String {
        val rule = match(tyep)
        return rule.encodeDescription(this)
    }

    fun <T : Any> match(type: TypeSpecification<T>): TranscodingRules.Rule<T> {
        val matchedCustomRule = customRules.firstOrNull { it.targetType == type.klazz }
            ?: customRules.firstOrNull { type.klazz.isSubclassOf(it.targetType) }
        if (matchedCustomRule != null) {
            return matchedCustomRule as TranscodingRules.CustomRule<T>
        }
        return TranscodingRules.matchBuiltInRule(type)
    }
}
