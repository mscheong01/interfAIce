package io.github.mscheong01.interfaice

import kotlin.reflect.KClass

object TranscodingRules {

    val BYTE = KotlinDefaultRule(
        type = Byte::class,
        encodeDescription = """
            a number literal ranging from -128 to 127, inclusive.
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toByte() }
    )

    val SHORT = KotlinDefaultRule(
        type = Short::class,
        encodeDescription = """
            a number literal ranging from -32768 to 32767, inclusive.
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toShort() }
    )

    val INT = KotlinDefaultRule(
        type = Int::class,
        encodeDescription = """
            a number literal ranging from -2147483648 to 2147483647, inclusive.
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toInt() }
    )

    val LONG = KotlinDefaultRule(
        type = Long::class,
        encodeDescription = """
            a number literal ranging from -9223372036854775808 to 9223372036854775807, inclusive.
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toLong() }
    )

    val FLOAT = KotlinDefaultRule(
        type = Float::class,
        encodeDescription = """
            a number literal ranging from -3.4028235E38 to 3.4028235E38, inclusive.
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toFloat() }
    )

    val DOUBLE = KotlinDefaultRule(
        type = Double::class,
        encodeDescription = """
            a number literal ranging from -1.7976931348623157E308 to 1.7976931348623157E308, inclusive.
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toDouble() }
    )

    val CHAR = KotlinDefaultRule(
        type = Char::class,
        encodeDescription = """
            a character literal, e.g. a
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it[0] }
    )

    val BOOLEAN = KotlinDefaultRule(
        type = Boolean::class,
        encodeDescription = """
            a boolean literal (choose exact value from true | false without capitalization)
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { it.toBoolean() }
    )

    val STRING = KotlinDefaultRule(
        type = String::class,
        encodeDescription = """
            a string literal, e.g. hello world (without surrounding quotes)
        """.trimIndent(),
        encoder = { it },
        decoder = { it }
    )

    fun <T : Any> match(type: KClass<T>): Rule<T> {
        return when (type) {
            Byte::class -> BYTE
            Short::class -> SHORT
            Int::class -> INT
            Long::class -> LONG
            Float::class -> FLOAT
            Double::class -> DOUBLE
            Char::class -> CHAR
            Boolean::class -> BOOLEAN
            String::class -> STRING
            else -> throw IllegalArgumentException("No default rule for type $type")
        } as Rule<T>
    }

    class KotlinDefaultRule<T : Any>(
        override val type: KClass<T>,
        override val encodeDescription: String,
        override val encoder: (T) -> String,
        override val decoder: (String) -> T
    ) : Rule<T>

    sealed interface Rule<T : Any> {
        val type: KClass<T>
        val encodeDescription: String
        val encoder: (@UnsafeVariance T) -> String
        val decoder: (String) -> T
    }
}
