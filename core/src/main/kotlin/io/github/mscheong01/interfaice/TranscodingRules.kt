package io.github.mscheong01.interfaice

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jsonMapper
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object TranscodingRules {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> matchBuiltInRule(type: TypeSpecification<T>): Rule<T> {
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
            type.klazz == LocalDateTime::class -> LOCAL_DATE_TIME
            type.klazz == LocalDate::class -> LOCAL_DATE
            type.klazz == LocalTime::class -> LOCAL_TIME
            type.klazz == Instant::class -> INSTANT
            type.klazz == Duration::class -> DURATION
            type.klazz == kotlin.time.Duration::class -> KOTLIN_DURATION
            type.isArray -> {
                val entryType = type.arrayTypeArgument
                ArrayRule(entryType)
            }
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

            type.klazz.isSubclassOf(Enum::class) -> EnumRule(type as TypeSpecification<out Enum<*>>)
            else -> ObjectRule(type)
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

    val LOCAL_DATE_TIME = KotlinDefaultRule(
        type = LocalDateTime::class,
        encodeDescription = """
            a datetime literal with format: yyyy-MM-dd HH:mm:ss
        """.trimIndent(),
        encoder = { it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) },
        decoder = { LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) }
    )

    val LOCAL_DATE = KotlinDefaultRule(
        type = LocalDate::class,
        encodeDescription = """
            a date literal with format: yyyy-MM-dd
        """.trimIndent(),
        encoder = { it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) },
        decoder = { LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
    )

    val LOCAL_TIME = KotlinDefaultRule(
        type = LocalTime::class,
        encodeDescription = """
            a time literal with format: HH:mm:ss
        """.trimIndent(),
        encoder = { it.format(DateTimeFormatter.ofPattern("HH:mm:ss")) },
        decoder = { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm:ss")) }
    )

    val INSTANT = KotlinDefaultRule(
        type = Instant::class,
        encodeDescription = """
            a datetime literal with format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { Instant.parse(it) }
    )

    val DURATION = KotlinDefaultRule(
        type = Duration::class,
        encodeDescription = """
            a duration literal with ISO-8601 duration format PnDTnHnMn.nS
        """.trimIndent(),
        encoder = { it.toString() },
        decoder = { Duration.parse(it) }
    )

    val KOTLIN_DURATION = KotlinDefaultRule(
        type = kotlin.time.Duration::class,
        encodeDescription = """
            a duration literal with ISO-8601 duration format PnDTnHnMn.nS
        """.trimIndent(),
        encoder = { it.toIsoString() },
        decoder = { kotlin.time.Duration.parse(it) }
    )

    class ArrayRule<T : Any>(
        val entryType: TypeSpecification<T>
    ) : Rule<Array<T>> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            return """
                    Json array with the following entry format:
                    %s
            """.trimIndent().format(
                transcoder.match(entryType).encodeDescription(transcoder)
            )
        }

        override fun encode(transcoder: TextObjectTranscoder, value: Array<T>): String {
            return ObjectRule.mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): Array<T> {
            val node = ObjectRule.mapper.readTree(value)
            require(node.isArray) { "expected json array. actual: $value" }
            val arrayNode = node as ArrayNode
            val list = arrayNode.map {
                if (it.isValueNode) {
                    transcoder.decode(it.asText(), entryType)
                } else {
                    transcoder.decode(it.toString(), entryType)
                }
            }
            val array = java.lang.reflect.Array.newInstance(entryType.klazz.java, list.size)
            list.forEachIndexed { index, entry -> java.lang.reflect.Array.set(array, index, entry) }
            return array as Array<T>
        }
    }

    class ListRule<T : Any>(
        override val entryType: TypeSpecification<T>
    ) : CollectionRule<T>(
        entryType = entryType
    ) {
        override fun decode(transcoder: TextObjectTranscoder, value: String): Collection<T> {
            val node = ObjectRule.mapper.readTree(value)
            require(node.isArray) { "expected json array. actual: $value" }
            val arrayNode = node as ArrayNode
            return arrayNode.map {
                if (it.isValueNode) {
                    transcoder.decode(it.asText(), entryType)
                } else {
                    transcoder.decode(it.toString(), entryType)
                }
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
            require(node.isArray) { "expected json array. actual: $value" }
            val arrayNode = node as ArrayNode
            return arrayNode.map {
                if (it.isValueNode) {
                    transcoder.decode(it.asText(), entryType)
                } else {
                    transcoder.decode(it.toString(), entryType)
                }
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
            """.trimIndent().format(
                transcoder.match(keyType).encodeDescription(transcoder),
                transcoder.match(valueType).encodeDescription(transcoder)
            )
        }

        override fun encode(transcoder: TextObjectTranscoder, value: Map<K, V>): String {
            return ObjectRule.mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): Map<K, V> {
            val node = ObjectRule.mapper.readTree(value)
            require(node.isObject) { "expected json object. actual: $value" }
            val objectNode = node as ObjectNode
            return objectNode.fields().asSequence().map { (k, v) ->
                transcoder.decode(k, keyType) to if (v.isValueNode) {
                    transcoder.decode(v.asText(), valueType)
                } else {
                    transcoder.decode(v.toString(), valueType)
                }
            }.toMap()
        }
    }

    sealed class CollectionRule<T : Any>(
        open val entryType: TypeSpecification<T>
    ) : Rule<Collection<T>> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            return """
                Json array with the following entry format:
                %s
            """.trimIndent().format(
                transcoder.match(entryType).encodeDescription(transcoder)
            )
        }

        override fun encode(transcoder: TextObjectTranscoder, value: Collection<T>): String {
            return ObjectRule.mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): Collection<T> {
            val node = ObjectRule.mapper.readTree(value)
            require(node.isArray) { "expected json array. actual: $value" }
            val arrayNode = node as ArrayNode
            return arrayNode.map {
                if (it.isValueNode) {
                    transcoder.decode(it.asText(), entryType)
                } else {
                    transcoder.decode(it.toString(), entryType)
                }
            }
        }
    }

    class EnumRule<T : Enum<T>>(
        val enumType: TypeSpecification<@UnsafeVariance T>
    ) : Rule<T> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            val enumConstants = enumType.klazz.java.enumConstants
            if (enumConstants != null) {
                return """
                    one of the following values:
                    %s
                """.trimIndent().format(
                    enumConstants.joinToString(", ") { it.name }
                )
            } else {
                throw IllegalArgumentException("enum type $enumType has no constants")
            }
        }

        override fun encode(transcoder: TextObjectTranscoder, value: T): String {
            return value.name
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): T {
            return enumType.klazz.java.enumConstants?.find { it.name == value }
                ?: throw IllegalArgumentException("no enum constant $value for type $enumType")
        }
    }

    class ObjectRule<T : Any>(
        val type: TypeSpecification<T>
    ) : Rule<T> {
        override fun encodeDescription(transcoder: TextObjectTranscoder): String {
            return """
                Json object with the following fields:
                %s
            """.trimIndent().format(
                type.klazz.java.declaredFields.joinToString("\n") { field ->
                    val typeSpecification = TypeSpecification(
                        klazz = field.type.kotlin,
                        javaType = field.genericType
                    )

                    """
                        %s -> {
                            %s
                        }
                    """.trimIndent().format(
                        field.name,
                        transcoder.match(typeSpecification).encodeDescription(transcoder)
                    )
                }
            )
        }

        override fun encode(transcoder: TextObjectTranscoder, value: T): String {
            return pureMapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): T {
            val node = mapper.readTree(value)
            require(node.isObject) { "expected json object. actual: $value" }
            val objectNode = node as ObjectNode

            val constructor = type.klazz.java.declaredConstructors.find { constructor ->
                constructor.parameters.all { parameter -> objectNode.has(parameter.name) }
            } ?: throw IllegalStateException("no constructor found for type ${type.klazz.java}")

            val parameters = constructor.parameters
            val arguments = parameters.map { parameter ->
                objectNode[parameter.name]?.let { valueNode ->
                    objectNode.remove(parameter.name)

                    val fieldType = TypeSpecification(
                        klazz = parameter.type.kotlin,
                        javaType = parameter.parameterizedType
                    )

                    if (valueNode.isValueNode) {
                        transcoder.decode(valueNode.asText(), fieldType)
                    } else {
                        transcoder.decode(valueNode.toString(), fieldType)
                    }
                }
            }.toTypedArray()

            constructor.isAccessible = true
            val obj: T
            try {
                @Suppress("UNCHECKED_CAST")
                obj = constructor.newInstance(*arguments) as T
            } finally {
                constructor.isAccessible = false
            }

            if (node.isEmpty) return obj

            val setters = type.klazz.java.methods.filter { it.name.startsWith("set") && it.parameterCount == 1 }
            setters.forEach { setter ->
                val fieldName = setter.name.substring(3).replaceFirstChar { it.lowercase() }
                objectNode[fieldName]?.let { valueNode ->
                    objectNode.remove(fieldName)

                    val fieldType = TypeSpecification(
                        klazz = setter.parameters[0].type.kotlin,
                        javaType = setter.parameters[0].parameterizedType
                    )
                    setter.invoke(
                        obj,
                        if (valueNode.isValueNode) {
                            transcoder.decode(valueNode.asText(), fieldType)
                        } else {
                            transcoder.decode(valueNode.toString(), fieldType)
                        }
                    )
                }
            }

            if (node.isEmpty) return obj

            type.klazz.java.declaredFields.forEach { field ->
                val fieldName = field.name
                objectNode[fieldName]?.let { valueNode ->
                    objectNode.remove(fieldName)

                    val fieldType = TypeSpecification(
                        klazz = field.type.kotlin,
                        javaType = field.genericType
                    )
                    field.isAccessible = true
                    try {
                        field.set(
                            obj,
                            if (valueNode.isValueNode) {
                                transcoder.decode(valueNode.asText(), fieldType)
                            } else {
                                transcoder.decode(valueNode.toString(), fieldType)
                            }
                        )
                    } finally {
                        field.isAccessible = false
                    }
                }
            }

            return obj
        }

        companion object {
            val mapper = jacksonObjectMapper()

            /**
             * Use pureMapper to prevent annotations like [com.fasterxml.jackson.databind.annotation.JsonNaming] from being used
             * and provide consistent serialized input for LLM.
             */
            val pureMapper = jsonMapper { disable(MapperFeature.USE_ANNOTATIONS) }
        }
    }

    abstract class CustomRule<T : Any>(
        val targetType: KClass<T>
    ) : Rule<T>

    sealed interface Rule<T : Any> {
        fun encodeDescription(transcoder: TextObjectTranscoder): String
        fun encode(transcoder: TextObjectTranscoder, value: @UnsafeVariance T): String
        fun decode(transcoder: TextObjectTranscoder, value: String): T
    }
}
