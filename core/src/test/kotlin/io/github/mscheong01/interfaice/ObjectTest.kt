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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjectTest {
    data class PersonInfo(
        val firstName: String,
        val lastName: String,
        val gender: Gender,
        val age: Int,
        val country: Country,
        val hobbies: List<String>
    )

    enum class Gender {
        MALE,
        FEMALE,
        OTHER
    }

    data class Country(
        val name: String,
        val city: String
    )

    interface TestInterface {
        @OpenAiChat
        fun getDummyPersonInfo(): PersonInfo

        @OpenAiChat
        fun getTwoDummyPersonInfos(): Map<String, PersonInfo>

        @OpenAiChat
        fun echoThisPersonInfo(personInfo: PersonInfo): PersonInfo
    }

    @Test
    fun testObjectReturnType() {
        val proxy = OpenAiProxyFactory
            .of(System.getenv("OPENAI_API_KEY"))
            .create<TestInterface>()

        val result = proxy.getDummyPersonInfo()
        assertThat(result.firstName).isNotBlank()
        assertThat(result.lastName).isNotBlank()
        assertThat(result.age).isPositive()
        assertThat(result.country.name).isNotBlank()
        assertThat(result.country.city).isNotBlank()
        assertThat(result.hobbies).isNotEmpty
        assertThat(result.hobbies.first()).isNotBlank()
    }

    @Test
    fun testMapOfObjectReturnType() {
        val proxy = OpenAiProxyFactory
            .of(System.getenv("OPENAI_API_KEY"))
            .create<TestInterface>()

        val result = proxy.getTwoDummyPersonInfos()
        assertThat(result).hasSize(2)
        result.forEach { (key, value) ->
            assertThat(key).isNotBlank()
            assertThat(value.firstName).isNotBlank()
            assertThat(value.lastName).isNotBlank()
            assertThat(value.age).isPositive()
            assertThat(value.country.name).isNotBlank()
            assertThat(value.country.city).isNotBlank()
            assertThat(value.hobbies).isNotEmpty
        }
    }

    @Test
    fun testObjectParameter() {
        val proxy = OpenAiProxyFactory
            .of(System.getenv("OPENAI_API_KEY"))
            .create<TestInterface>()

        val personInfo = PersonInfo(
            firstName = "John",
            lastName = "Doe",
            gender = Gender.MALE,
            age = 30,
            country = Country(
                name = "South Korea",
                city = "Seoul"
            ),
            hobbies = listOf("Reading", "Writing", "Coding")
        )
        val result = proxy.echoThisPersonInfo(personInfo)
        assertThat(result).isEqualTo(personInfo)
    }
}
