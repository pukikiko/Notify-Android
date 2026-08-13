package com.notify.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * The Notify backend uses numeric ids for library rows ("12") and string ids
 * for discover/catalog placeholders ("track:abc123", "catalog-artist:Daft Punk").
 * This serializer transparently reads either and always exposes a String.
 */
object FlexibleIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        return when (val el: JsonElement = json.decodeJsonElement()) {
            is JsonPrimitive -> el.jsonPrimitive.content
            else -> el.toString()
        }
    }
}
