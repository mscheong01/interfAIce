package io.github.mscheong01.interfaice.openai

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

@Target
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class OpenAiInterface(
    @get:AliasFor(annotation = Component::class)
    val value: String = ""
)
