package io.github.mscheong01.interfaice

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

    open class CollectionRule<T : Any>(
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
            return mapper.writeValueAsString(value)
        }

        override fun decode(transcoder: TextObjectTranscoder, value: String): T {
            val node = mapper.readTree(value)
            require(node.isObject) { "expected json object. actual: $value" }
            val objectNode = node as ObjectNode

            return type.klazz.java.declaredConstructors.find { it.parameterCount == 0 }?.let { noArgsConstructor ->
                noArgsConstructor.isAccessible = true
                val obj: T
                try {
                    @Suppress("UNCHECKED_CAST")
                    obj = noArgsConstructor.newInstance() as T
                } finally {
                    noArgsConstructor.isAccessible = false
                }

                type.klazz.java.declaredFields.forEach { field ->
                    val valueNode = objectNode[field.name]
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
                obj
            } ?: run {
                val constructor = type.klazz.java.declaredConstructors.find { constructor ->
                    constructor.parameters.all { parameter -> objectNode.has(parameter.name) }
                } ?: throw IllegalStateException(
                    """
                        No suitable constructor found for type ${type.klazz.java}.
                        Expected a constructor with no parameters or with parameters matching the fields of the json object.
                        If you want to use one with parameters, the compile option '-parameters' must be enabled.
                    """.trimIndent()
                )

                val parameters = constructor.parameters
                val arguments = parameters.map { parameter ->
                    val valueNode = objectNode[parameter.name]
                    val fieldType = TypeSpecification(
                        klazz = parameter.type.kotlin,
                        javaType = parameter.parameterizedType
                    )

                    if (valueNode.isValueNode) {
                        transcoder.decode(valueNode.asText(), fieldType)
                    } else {
                        transcoder.decode(valueNode.toString(), fieldType)
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
                obj
            }
        }

        companion object {
            val mapper = jacksonObjectMapper()
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
