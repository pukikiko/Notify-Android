package com.notify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Artist(
    @Serializable(with = FlexibleIdSerializer::class) val id: String = "",
    val name: String,
    val mbid: String? = null,
    val image: String? = null,
    val genres: List<String> = emptyList(),
    val similar: List<String> = emptyList(),
    val liked: Boolean? = null,
    @SerialName("trackCount") val trackCount: Int? = null,
    @SerialName("albumCount") val albumCount: Int? = null,
    val popularity: Int? = null,
    val bio: String? = null,
    @SerialName("wikiImage") val wikiImage: String? = null,
    val href: String? = null,
    @SerialName("libraryId") val libraryId: String? = null,
    val kind: String? = null
)

@Serializable
data class Album(
    @Serializable(with = FlexibleIdSerializer::class) val id: String = "",
    val title: String,
    val mbid: String? = null,
    val year: Int? = null,
    val image: String? = null,
    val genres: List<String> = emptyList(),
    val liked: Boolean? = null,
    val artist: MiniArtist? = null,
    @SerialName("trackCount") val trackCount: Int? = null,
    val href: String? = null,
    @SerialName("libraryId") val libraryId: String? = null,
    @SerialName("albumType") val albumType: String? = null,
    val kind: String? = null,
    val source: JsonElement? = null
)

@Serializable
data class MiniArtist(
    @SerialName("name") val name: String,
    @Serializable(with = FlexibleIdSerializer::class) val id: String? = null,
    val mbid: String? = null
)

@Serializable
data class MiniAlbum(
    val title: String? = null,
    @Serializable(with = FlexibleIdSerializer::class) val id: String? = null,
    val mbid: String? = null,
    val year: Int? = null,
    val image: String? = null
)

@Serializable
data class Track(
    @Serializable(with = FlexibleIdSerializer::class) val id: String,
    val title: String,
    val duration: Double? = null,
    val bitrate: Int? = null,
    @SerialName("sourceFormat") val sourceFormat: String? = null,
    val size: Long? = null,
    val mbid: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val source: JsonElement? = null,
    val username: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val liked: Boolean? = null,
    val artist: Artist? = null,
    val album: Album? = null,
    @SerialName("artUrl") val artUrl: String? = null,
    @SerialName("streamUrl") val streamUrl: String? = null,
    val image: String? = null,
    @SerialName("trackNo") val trackNo: Int? = null,
    val provider: String? = null,
    val downloaded: Boolean? = null,
    val kind: String? = null,
    val popularity: Int? = null
) {
    val displayArtist: String
        get() = artist?.name ?: ""
    val displayAlbum: String
        get() = album?.title ?: ""
    val displayArt: String?
        get() = artUrl ?: album?.image ?: image

    /** A real, playable library row carries a numeric id or a streamUrl. */
    val isResolved: Boolean
        get() = id.toLongOrNull() != null || !streamUrl.isNullOrEmpty()
}
