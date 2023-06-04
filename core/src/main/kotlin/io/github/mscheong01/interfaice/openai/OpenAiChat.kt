package io.github.mscheong01.interfaice.openai

@Target(AnnotationTarget.FUNCTION)
annotation class OpenAiChat(
    val model: String = "gpt-3.5-turbo",
    val description: String = ""
)
