package io.github.mscheong01.interfaice

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object TranscodingRules {

    fun <T : Any> match(type: TypeSpecification<T>): Rule<T> {
        return when {
            type.klazz == Byte::class -> BYTE
            type.klazz == Short::class -> SHORT
            type.klazz == Int::class -> INT
            type.klazz == Long::class -> LONG
            type.klazz == Float::class -> FLOAT
            type.klazz == Double::class -> DOUBLE
            type.klazz == Char::class -> CHAR
            type.klazz == Boolean::class -> BOOLEAN
            type.klazz == String::class -> STRING
            type.klazz.isSubclassOf(Collection::class) -> {
                val entryType = type.typeArguments.first()
                when {
                    type.klazz.isSubclassOf(List::class) -> ListRule(entryType)
                    type.klazz.isSubclassOf(Set::class) -> SetRule(entryType)
                    else -> throw IllegalArgumentException("unsupported type: $type")
                }
            }
            type.klazz.isSubclassOf(Map::class) -> {
                val keyType = type.typeArguments[0]
                val valueType = type.typeArguments[1]
                MapRule(keyType, valueType)
            }
            else -> {
                throw IllegalArgumentException("unsupported type: ${type.klazz.qualifiedName}")
            }
        } as Rule<T>
    }

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
            a single character literal, e.g. a
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

    class ListRule<T : Any>(
        override val entryType: TypeSpecification<T>
    ) : CollectionRule<T>(
        entryType = entryType
    ) {
        override fun decode(transcoder: TextObjectTranscoder, value: String): Collection<T> {
            val node = ObjectRule.mapper.readTree(value)
            if (!node.isArray) {
                throw IllegalArgumentException("expected json array. actual: $value")
            }
            val arrayNode = node as ArrayNode
            return arrayNode.map {
                transcoder.decode(it.asText(), entryType)
            }
        }
    }

    class SetRule<T : Any>(
        override val entryType: TypeSpecification<T>
    ) : CollectionRule<T>(
        entryType = entryType
    ) {
        override fun decode(transcoder: TextObjectTranscoder, value: String): Collection<T> {
            val node = ObjectRule.mapper.readTree(value)
            if (!node.isArray) {
                throw IllegalArgumentException("expected json array. actual: $value")
            }
            val arrayNode = node as ArrayNode
            return arrayNode.map {
                transcoder.decode(it.asText(), entryType)
            }.toSet()
        }
    }

    class KotlinDefaultRule<T : Any>(
        val type: KClass<T>,
        val encodeDescription: String,
        val encoder: (T) -> String,
        val decoder: (String) -> T
    ) : Rule<T> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            return encodeDescription
        }

        override fun encode(transcoder: TextObjectTranscoder, value: T): String {
            return encoder(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): T {
            return decoder(value)
        }
    }

    class MapRule<K : Any, V : Any>(
        val keyType: TypeSpecification<K>,
        val valueType: TypeSpecification<V>
    ) : Rule<Map<K, V>> {

        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            return """
                Json object with the following key/value format:
                key -> {
                    %s
                }
                value -> {
                    %s
                }
            """.format(
                transcoder.match(keyType).encodeDescription(transcoder),
                transcoder.match(valueType).encodeDescription(transcoder)
            ).trimIndent()
        }

        override fun encode(transcoder: TextObjectTranscoder, value: Map<K, V>): String {
            return ObjectRule.mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): Map<K, V> {
            val node = ObjectRule.mapper.readTree(value)
            if (!node.isObject) {
                throw IllegalArgumentException("expected json object. actual: $value")
            }
            val objectNode = node as ObjectNode
            return objectNode.fields().asSequence().map { (key, value) ->
                transcoder.decode(key, keyType) to transcoder.decode(value.asText(), valueType)
            }.toMap()
        }
    }
    open class CollectionRule<T : Any>(
        open val entryType: TypeSpecification<T>
    ) : Rule<Collection<T>> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            return """
                Json array with the following entry format:
                %s
            """.format(
                transcoder.match(entryType).encodeDescription(transcoder)
            ).trimIndent()
        }

        override fun encode(transcoder: TextObjectTranscoder, value: Collection<T>): String {
            return ObjectRule.mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): Collection<T> {
            val node = ObjectRule.mapper.readTree(value)
            if (!node.isArray) {
                throw IllegalArgumentException("expected json array. actual: $value")
            }
            val arrayNode = node as ArrayNode
            return arrayNode.map {
                transcoder.decode(it.asText(), entryType)
            }
        }
    }

    class ObjectRule<T : Any> : Rule<T> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            TODO("Not yet implemented")
        }

        override fun encode(transcoder: TextObjectTranscoder, value: T): String {
            return mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): T {
            TODO("Not yet implemented")
        }
        companion object {
            val mapper = jacksonObjectMapper()
        }
    }

    abstract class CustomRule<T : Any>(
        val matchType: KClass<T>
    ) : Rule<T>

    sealed interface Rule<T : Any> {
        fun encodeDescription(transcoder: TextObjectTranscoder): String
        fun encode(transcoder: TextObjectTranscoder, value: @UnsafeVariance T): String
        fun decode(transcoder: TextObjectTranscoder, value: String): T
    }
}
