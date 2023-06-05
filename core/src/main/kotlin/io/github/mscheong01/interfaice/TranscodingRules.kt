package io.github.mscheong01.interfaice

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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
        entryType = entryType,
        decoder = {
            val entryRule = match(entryType)
            val node = ObjectRule.mapper.readTree(it)
            if (!node.isArray) {
                throw IllegalArgumentException("expected json array. actual: $it")
            }
            val arrayNode = node as ArrayNode
            arrayNode.map {
                entryRule.decoder(it.asText())
            }
        }
    )

    class SetRule<T : Any>(
        override val entryType: TypeSpecification<T>
    ) : CollectionRule<T>(
        entryType = entryType,
        decoder = {
            val entryRule = match(entryType)
            val node = ObjectRule.mapper.readTree(it)
            if (!node.isArray) {
                throw IllegalArgumentException("expected json array. actual: $it")
            }
            val arrayNode = node as ArrayNode
            arrayNode.map {
                entryRule.decoder(it.asText())
            }.toSet()
        }
    )

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
                throw IllegalArgumentException("unsupported type: $type")
            }
        } as Rule<T>
    }

    class KotlinDefaultRule<T : Any>(
        val type: KClass<T>,
        override val encodeDescription: String,
        override val encoder: (T) -> String,
        override val decoder: (String) -> T
    ) : Rule<T>

    class MapRule<K : Any, V : Any>(
        val keyType: TypeSpecification<K>,
        val valueType: TypeSpecification<V>,
        override val encodeDescription: String = """
            Json object with the following key/value format:
            key -> {
                %s
            }
            value -> {
                %s
            }
        """.format(
            match(keyType).encodeDescription,
            match(valueType).encodeDescription
        ).trimIndent(),
        override val encoder: (Map<K, V>) -> String = {
            ObjectRule.mapper.writeValueAsString(it)
        },
        override val decoder: (String) -> Map<K, V> = {
            val keyRule = match(keyType)
            val valueRule = match(valueType)
            val node = ObjectRule.mapper.readTree(it)
            if (!node.isObject) {
                throw IllegalArgumentException("expected json object. actual: $it")
            }
            val objectNode = node as ObjectNode
            objectNode.fields().asSequence().map { (key, value) ->
                keyRule.decoder(key) to valueRule.decoder(value.asText())
            }.toMap()
        }
    ) : Rule<Map<K, V>> {
        init {
            if (match(keyType) !is KotlinDefaultRule) {
                throw IllegalArgumentException("key type must be a Kotlin default type")
            }
        }
    }
    open class CollectionRule<T : Any>(
        open val entryType: TypeSpecification<T>,
        override val encodeDescription: String = """
            Json array with the following entry format:
            %s
        """.format(
            match(entryType).encodeDescription
        ).trimIndent(),
        override val encoder: (Collection<T>) -> String = { ObjectRule.mapper.writeValueAsString(it) },
        override val decoder: (String) -> Collection<T>
    ) : Rule<Collection<T>>

    class ObjectRule<T : Any>(
        override val encodeDescription: String,
        override val encoder: (T) -> String,
        override val decoder: (String) -> T
    ) : Rule<T> {
        companion object {
            val mapper = jacksonObjectMapper()
        }
    }

    sealed interface Rule<T : Any> {
        val encodeDescription: String
        val encoder: (@UnsafeVariance T) -> String
        val decoder: (String) -> T
    }
}
