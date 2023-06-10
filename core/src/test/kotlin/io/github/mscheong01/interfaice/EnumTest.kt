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

class EnumTest {
    enum class School {
        ELEMENTARY,
        MIDDLE,
        HIGH,
        NOT_STUDENT
    }

    interface TestInterface {
        @OpenAiChat
        fun appropriateSchoolForAge(age: Int): School
    }

    @Test
    fun testEnum() {
        val proxy = OpenAiProxyFactory
            .of(System.getenv("OPENAI_API_KEY"))
            .create<TestInterface>()

        val result = proxy.appropriateSchoolForAge(8)
        assertThat(result).isEqualTo(School.ELEMENTARY)
        val result2 = proxy.appropriateSchoolForAge(14)
        assertThat(result2).isEqualTo(School.MIDDLE)
        val result3 = proxy.appropriateSchoolForAge(17)
        assertThat(result3).isEqualTo(School.HIGH)
        val result4 = proxy.appropriateSchoolForAge(25)
        assertThat(result4).isEqualTo(School.NOT_STUDENT)
    }
}
