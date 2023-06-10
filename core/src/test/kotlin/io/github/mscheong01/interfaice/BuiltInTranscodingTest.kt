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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

class BuiltInTranscodingTest {
    val transcoder = TextObjectTranscoder()

    @Test
    fun testTime() {
        val dateTime = LocalDateTime.of(2021, 1, 1, 0, 0, 0)
        val encodedDateTime = transcoder.encode(dateTime)
        Assertions.assertThat(encodedDateTime).isEqualTo("2021-01-01 00:00:00")
        val decodedDateTime = transcoder.decode(encodedDateTime, TypeSpecification.from(dateTime))
        Assertions.assertThat(decodedDateTime).isEqualTo(dateTime)

        val date = LocalDate.of(2021, 1, 1)
        val encodedDate = transcoder.encode(date)
        Assertions.assertThat(encodedDate).isEqualTo("2021-01-01")
        val decodedDate = transcoder.decode(encodedDate, TypeSpecification.from(date))
        Assertions.assertThat(decodedDate).isEqualTo(date)

        val time = LocalTime.of(3, 56)
        val encodedTime = transcoder.encode(time)
        Assertions.assertThat(encodedTime).isEqualTo("03:56:00")
        val decodedTime = transcoder.decode(encodedTime, TypeSpecification.from(time))
        Assertions.assertThat(decodedTime).isEqualTo(time)

        val duration = Duration.ofMinutes(3)
        val encodedDuration = transcoder.encode(duration)
        Assertions.assertThat(encodedDuration).isEqualTo("PT3M")
        val decodedDuration = transcoder.decode(encodedDuration, TypeSpecification.from(duration))
        Assertions.assertThat(decodedDuration).isEqualTo(duration)

        val kotlinDuration = 3.minutes
        val encodedKotlinDuration = transcoder.encode(kotlinDuration)
        Assertions.assertThat(encodedKotlinDuration).isEqualTo("PT3M")
        val decodedKotlinDuration = transcoder.decode(encodedKotlinDuration, TypeSpecification.from(kotlinDuration))
        Assertions.assertThat(decodedKotlinDuration).isEqualTo(kotlinDuration)
    }
}
