package io.github.mscheong01.interfaice.openai

@Target(AnnotationTarget.FUNCTION)
annotation class OpenAiChat(
    val model: String = DEFAULT_MODEL,
    val description: String = ""
) {
    companion object {
        const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }
}
