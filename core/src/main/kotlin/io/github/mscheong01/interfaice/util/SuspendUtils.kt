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
package io.github.mscheong01.interfaice.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> CoroutineContext.invokeSuspending(
    continuation: Continuation<T>,
    block: suspend () -> T
): Any {
    val deferred = CoroutineScope(this).async {
        block()
    }

    deferred.invokeOnCompletion { cause: Throwable? ->
        if (cause != null) {
            continuation.resumeWithException(cause)
        } else {
            continuation.resume(deferred.getCompleted())
        }
    }

    return kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
}
