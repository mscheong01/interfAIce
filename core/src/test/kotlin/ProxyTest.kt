import io.github.mscheong01.interfaice.openai.OpenAiChat
import io.github.mscheong01.interfaice.openai.OpenAiProxy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

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
    val proxy = OpenAiProxy(System.getenv("OPENAI_API_KEY")).of(TestInterface::class.java)

    @Test
    fun test() {
        val result = proxy.randomMountainInCountry("Nepal")
        println(result)
        val result2 = proxy.multiply(16, 8)
        println(result2)
    }

    @Test
    fun suspendTest(): Unit = runBlocking {
        val result = proxy.randomCityInCountry("Japan")
        println(result)
    }

    interface TestInterface {
        @OpenAiChat
        fun randomMountainInCountry(countryName: String): String

        @OpenAiChat
        fun multiply(a: Int, b: Int): Int

        @OpenAiChat
        suspend fun randomCityInCountry(countryName: String): String
    }
}
