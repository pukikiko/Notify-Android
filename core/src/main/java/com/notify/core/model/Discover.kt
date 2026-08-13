package com.notify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Top-level entity of a discover response. */
@Serializable
data class DiscoverArtist(
    @Serializable(with = FlexibleIdSerializer::class) val id: String? = null,
    val name: String,
    val mbid: String? = null,
    val genres: List<String> = emptyList(),
    val image: String? = null,
    val popularity: Int? = null,
    val href: String? = null,
    @SerialName("libraryId") val libraryId: String? = null,
    val kind: String? = null
)

@Serializable
data class DiscoverAlbum(
    @Serializable(with = FlexibleIdSerializer::class) val id: String? = null,
    val title: String,
    val mbid: String? = null,
    val year: Int? = null,
    val image: String? = null,
    val artist: MiniArtist? = null,
    @SerialName("trackCount") val trackCount: Int? = null,
    val href: String? = null,
    @SerialName("libraryId") val libraryId: String? = null,
    val kind: String? = null,
    val albumType: String? = null
)

@Serializable
data class DiscoverPlaylist(
    @Serializable(with = FlexibleIdSerializer::class) val id: String,
    val name: String,
    val mbid: String? = null,
    val image: String? = null,
    val owner: String? = null,
    @SerialName("ownerId") val ownerId: String? = null,
    val description: String? = null,
    @SerialName("trackCount") val trackCount: Int? = null,
    val followers: Int? = null,
    @SerialName("spotifyUrl") val spotifyUrl: String? = null,
    val kind: String? = null
)

@Serializable
data class DiscoverResult(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val playlists: List<DiscoverPlaylist> = emptyList(),
    @SerialName("popularTracks") val popularTracks: List<Track> = emptyList(),
    val artist: DiscoverArtist? = null,
    @SerialName("webTracks") val webTracks: List<Track> = emptyList(),
    val fallback: Boolean = false,
    val degraded: Boolean = false,
    val error: String? = null
)

@Serializable
data class DiscoverArtistDetail(
    val artist: DiscoverArtist,
    val tracks: List<Track> = emptyList(),
    @SerialName("popularTracks") val popularTracks: List<Track> = emptyList(),
    val albums: List<DiscoverAlbum> = emptyList()
)

@Serializable
data class DiscoverAlbumDetail(
    val album: DiscoverAlbum,
    val tracks: List<Track> = emptyList()
)

@Serializable
data class DiscoverPlaylistDetail(
    val playlist: DiscoverPlaylist,
    val tracks: List<Track> = emptyList()
)

@Serializable
data class DiscoverUserDetail(
    val user: DiscoverUser,
    val playlists: List<DiscoverPlaylist> = emptyList()
)

@Serializable
data class DiscoverUser(
    @Serializable(with = FlexibleIdSerializer::class) val id: String? = null,
    val name: String,
    val image: String? = null,
    val followers: Long? = null,
    @SerialName("spotifyUrl") val spotifyUrl: String? = null,
    val kind: String? = null
)

@Serializable
data class PlayResponse(
    val tracks: List<Track> = emptyList(),
    val download: Boolean = false
)

/** Body for POST /discover/play */
@Serializable
data class DiscoverPlayRequest(
    val kind: String? = null,
    val source: kotlinx.serialization.json.JsonElement? = null,
    val artist: String? = null,
    val album: String? = null,
    val title: String? = null,
    val mbid: String? = null,
    @SerialName("releaseMbid") val releaseMbid: String? = null,
    val image: String? = null,
    val duration: Double? = null
)
