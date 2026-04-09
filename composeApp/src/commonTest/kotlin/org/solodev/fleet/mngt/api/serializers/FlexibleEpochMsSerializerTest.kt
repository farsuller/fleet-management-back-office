package org.solodev.fleet.mngt.api.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlexibleEpochMsSerializerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TimestampHolder(
        @Serializable(with = FlexibleEpochMsSerializer::class)
        val value: Long?,
    )

    @Test
    fun shouldDeserializeEpochMilliseconds() {
        val decoded = json.decodeFromString<TimestampHolder>("{\"value\":1739174400000}")

        assertEquals(1739174400000L, decoded.value)
    }

    @Test
    fun shouldDeserializeNumericStringEpoch() {
        val decoded = json.decodeFromString<TimestampHolder>("{\"value\":\"1739174400000\"}")

        assertEquals(1739174400000L, decoded.value)
    }

    @Test
    fun shouldDeserializeIso8601String() {
        val decoded = json.decodeFromString<TimestampHolder>("{\"value\":\"2026-02-10T10:00:00Z\"}")

        assertEquals(1770717600000L, decoded.value)
    }

    @Test
    fun shouldDeserializeNullAndInvalidValues() {
        val nullDecoded = json.decodeFromString<TimestampHolder>("{\"value\":null}")
        val invalidDecoded = json.decodeFromString<TimestampHolder>("{\"value\":\"not-a-date\"}")

        assertNull(nullDecoded.value)
        assertNull(invalidDecoded.value)
    }

    @Test
    fun shouldSerializeLongAndNullValues() {
        val encodedLong = json.encodeToString(TimestampHolder(1739174400000L))
        val encodedNull = json.encodeToString(TimestampHolder(null))

        assertEquals("{\"value\":1739174400000}", encodedLong)
        assertEquals("{\"value\":null}", encodedNull)
    }

    @Test
    fun shouldFallbackToPlainLongDecode_ForNonJsonDecoder() {
        val decoded = FlexibleEpochMsSerializer.deserialize(LongOnlyDecoder(42L))

        assertEquals(42L, decoded)
    }

    private class LongOnlyDecoder(
        private val value: Long,
    ) : Decoder {
        override val serializersModule = Json.serializersModule

        override fun decodeLong(): Long = value

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = throw UnsupportedOperationException()
        override fun decodeBoolean(): Boolean = throw UnsupportedOperationException()
        override fun decodeByte(): Byte = throw UnsupportedOperationException()
        override fun decodeChar(): Char = throw UnsupportedOperationException()
        override fun decodeDouble(): Double = throw UnsupportedOperationException()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = throw UnsupportedOperationException()
        override fun decodeFloat(): Float = throw UnsupportedOperationException()
        override fun decodeInline(descriptor: SerialDescriptor): Decoder = this
        override fun decodeInt(): Int = throw UnsupportedOperationException()
        override fun decodeNotNullMark(): Boolean = true
        override fun decodeNull(): Nothing? = null
        override fun decodeShort(): Short = throw UnsupportedOperationException()
        override fun decodeString(): String = throw UnsupportedOperationException()
    }
}