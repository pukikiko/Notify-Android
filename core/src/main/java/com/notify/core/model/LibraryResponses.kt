package com.notify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeData(
    @SerialName("popularTracks") val popularTracks: List<Track> = emptyList(),
    @SerialName("popularAlbums") val popularAlbums: List<Album> = emptyList(),
    @SerialName("popularArtists") val popularArtists: List<Artist> = emptyList(),
    @SerialName("recentAlbums") val recentAlbums: List<Album> = emptyList(),
    @SerialName("recentTracks") val recentTracks: List<Track> = emptyList(),
    val liked: List<Track> = emptyList()
)

@Serializable
data class TracksResponse(
    val tracks: List<Track> = emptyList()
)

@Serializable
data class TrackResponse(
    val track: Track
)

@Serializable
data class AlbumsResponse(
    val albums: List<Album> = emptyList()
)

@Serializable
data class AlbumDetailResponse(
    val album: Album,
    val tracks: List<Track> = emptyList()
)

@Serializable
data class ArtistsResponse(
    val artists: List<Artist> = emptyList()
)

@Serializable
data class ArtistDetailResponse(
    val artist: Artist,
    val tracks: List<Track> = emptyList(),
    @SerialName("popularTracks") val popularTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList()
)

@Serializable
data class SearchResponse(
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList()
)

@Serializable
data class LikeResponse(
    val liked: Boolean
)
