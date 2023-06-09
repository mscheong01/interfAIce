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
package io.github.mscheong01.interfaice.examples

import io.github.mscheong01.interfaice.create
import io.github.mscheong01.interfaice.openai.OpenAiChat

interface GreetingService {
    @OpenAiChat(
        description = "A delightful greeting message that greets all open source enthusiasts to interfaice"
    )
    fun greet(name: String): String
}

fun main() {
    val proxy = io.github.mscheong01.interfaice.openai.OpenAiProxyFactory
        .of(System.getenv("OPENAI_API_KEY"))
        .create<GreetingService>()
    val result = proxy.greet("Sam Altman")
    println(result)
}
