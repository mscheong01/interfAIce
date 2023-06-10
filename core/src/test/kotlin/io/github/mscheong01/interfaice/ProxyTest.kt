package io.github.mscheong01.interfaice

import io.github.mscheong01.interfaice.openai.OpenAiChat
import io.github.mscheong01.interfaice.openai.OpenAiProxyFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
class ProxyTest {
    val proxy = OpenAiProxyFactory.of(System.getenv("OPENAI_API_KEY")).create(TestInterface::class.java)

    @Test
    fun test() {
        val result = proxy.randomMountainInCountry("Nepal")
        println(result)
        val result2 = proxy.multiply(16, 8)
        println(result2)
    }

    @Test
    fun testTime() {
        val timeProxy = OpenAiProxyFactory.of(System.getenv("OPENAI_API_KEY")).create(TimeTestInterface::class.java)
        val dateTime = timeProxy.randomDateTime()
        println(dateTime)
        val date = timeProxy.randomDate()
        println(date)
        val time = timeProxy.randomTime()
        println(time)
        val duration = timeProxy.randomDuration()
        println(duration)
        val kotlinDuration = timeProxy.randomKDuration()
        println(kotlinDuration)
    }

    @Test
    fun suspendTest(): Unit = runBlocking {
        val result = proxy.randomCityInCountry("Japan")
        println(result)
    }

    @Test
    fun nullableTest() {
        val result = proxy.greetingMessage("John")
        Assertions.assertThat(result).contains("John")
        println(result)
        val result2 = proxy.greetingMessage(null)
        Assertions.assertThat(result2).doesNotContain("NULL")
        println(result2)
    }

    @Test
    fun collectionTest() {
        val result = proxy.listOfMountainNamesInCountry("Nepal")
        println(result)
        val result2 = proxy.setOfRandomNumbersFrom1To100(10)
        println(result2)
    }

    @Test
    fun suspendCollectionTest(): Unit = runBlocking {
        val result = proxy.listOfCityNamesInCountry("Korea")
        println(result)
    }

    @Test
    fun mapTest() {
        val result = proxy.mapOfCountryNameToCapitalName("Asia")
        println(result)
    }

    @Test
    fun suspendMapTest(): Unit = runBlocking {
        val result = proxy.mapOfCountryNameToPopulation("Asia")
        println(result)
    }

    @Test
    fun monoTest(): Unit = runBlocking {
        val result = proxy.randomGreetingMessage("Seoul National University").awaitSingle()
        println(result)
    }

    @Test
    fun fluxTest(): Unit = runBlocking {
        val result = proxy.randomGreetingMessages("Seoul National University").collectList().awaitSingle()
        println(result)
    }

    @Test
    fun flowTest(): Unit = runBlocking {
        val result = proxy.randomWelcomeMessages("Seoul National University").toList()
        println(result)
    }

    interface TestInterface {

        @OpenAiChat
        fun randomMountainInCountry(countryName: String): String

        @OpenAiChat
        fun multiply(a: Int, b: Int): Int

        @OpenAiChat
        suspend fun randomCityInCountry(countryName: String): String

        @OpenAiChat
        fun greetingMessage(name: String?): String

        @OpenAiChat
        fun listOfMountainNamesInCountry(countryName: String): List<String>

        @OpenAiChat
        fun setOfRandomNumbersFrom1To100(count: Int): Set<Int>

        @OpenAiChat(
            description = "Returns a list of city names in the given country. " +
                "return names with according language of that country."
        )
        suspend fun listOfCityNamesInCountry(countryName: String): List<String>

        @OpenAiChat
        fun mapOfCountryNameToCapitalName(continentName: String): Map<String, String>

        @OpenAiChat
        suspend fun mapOfCountryNameToPopulation(continentName: String): Map<String, Int>

        @OpenAiChat
        fun randomGreetingMessage(fromSchool: String, count: Int = 3): Mono<String>

        @OpenAiChat
        fun randomGreetingMessages(fromSchool: String, count: Int = 3): Flux<String>

        @OpenAiChat
        fun randomWelcomeMessages(fromCompany: String, count: Int = 5): Flow<String>
    }

    interface TimeTestInterface {
        fun randomDateTime(): LocalDateTime
        fun randomDate(): LocalDate
        fun randomTime(): LocalTime
        fun randomDuration(): Duration

        fun randomKDuration(): kotlin.time.Duration
    }
}
