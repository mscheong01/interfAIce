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

import io.github.mscheong01.interfaice.openai.OpenAiChat
import io.github.mscheong01.interfaice.openai.OpenAiProxyFactory
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CustomTranscodingRuleTest {
    interface TestInterface {
        @OpenAiChat
        fun randomHistoricalEventsAroundDate(date: LocalDate): List<String>

        @OpenAiChat
        fun dateOfHistoricalEvent(eventName: String): LocalDate
    }

    @Test
    fun testDate() {
        println(LocalDate.ofYearDay(1914, 1))
    }

    @Test
    fun testCustomTranscodingRule() {
        val customTranscodingRule = object : TranscodingRules.CustomRule<LocalDate>(
            matchType = LocalDate::class
        ) {
            override fun encodeDescription(transcoder: TextObjectTranscoder): String {
                return "A date lieteral in the format of YYYY-MM-DD"
            }
            override fun encode(transcoder: TextObjectTranscoder, value: LocalDate): String {
                return value.toString()
            }
            override fun decode(transcoder: TextObjectTranscoder, value: String): LocalDate {
                return LocalDate.parse(value)
            }
        }
        val proxy = OpenAiProxyFactory
            .of(System.getenv("OPENAI_API_KEY"), listOf(customTranscodingRule))
            .create(TestInterface::class.java)
        val result = proxy.randomHistoricalEventsAroundDate(LocalDate.ofYearDay(1914, 1))
        println(result)
        val result2 = proxy.dateOfHistoricalEvent("Start of world war 1")
        println(result2)
    }
}
