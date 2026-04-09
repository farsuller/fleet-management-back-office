package org.solodev.fleet.mngt.api.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

/**
 * Deserializes a Long? timestamp that may arrive from the backend either as:
 *   - a numeric epoch-millisecond value:  1739174400000
 *   - an ISO 8601 string:                 "2026-02-10T10:00:00Z"
 *
 * Serializes back as a plain Long.
 */
object FlexibleEpochMsSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor = Long.serializer().nullable.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: Long?,
    ) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long? {
        // For non-JSON decoders fall back to a plain Long decode
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: return runCatching { decoder.decodeLong() }.getOrNull()

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive ->
                when {
                    element.isString ->
                        // Try numeric string first ("1739174400000"), then ISO-8601
                        element.content.toLongOrNull()
                            ?: runCatching { Instant.parse(element.content).toEpochMilliseconds() }.getOrNull()
                    else -> element.longOrNull
                }
            else -> null
        }
    }
}
