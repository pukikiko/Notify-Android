package com.notify.core.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.patch
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.notify.core.model.*

class ApiException(message: String, val status: Int) : Exception(message)

object JsonProvider {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

/** Thin wrapper over the Notify HTTP API. Base URL and auth token are read
 *  from the active instance on every call so instance switching is live. */
class NotifyApi(
    private val session: SessionManager,
    private val client: HttpClient = createClient()
) {

    private val base: String
        get() = session.baseUrl
    private val token: String?
        get() = session.token

    companion object {
        fun createClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(JsonProvider.json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
        }
    }

    private suspend inline fun <reified T> parse(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return response.body<T>()
        }
        val errBody = runCatching { response.bodyAsText() }.getOrNull()
        val message = runCatching {
            JsonProvider.json.decodeFromString<Map<String, String>>(errBody ?: "")["error"]
        }.getOrNull()
        throw ApiException(message ?: "Request failed (${response.status.value})", response.status.value)
    }

    private fun reqHeaders() = io.ktor.http.Headers.build {
        if (token != null) append(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
    }

    /* ---------------- auth ---------------- */

    suspend fun login(username: String, password: String): LoginResponse {
        val resp = client.post("$base/api/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(mapOf("username" to username, "password" to password))
        }
        return parse(resp)
    }

    suspend fun register(username: String, password: String): LoginResponse {
        val resp = client.post("$base/api/auth/register") {
            header(io.ktor.http.HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(mapOf("username" to username, "password" to password))
        }
        return parse(resp)
    }

    suspend fun me(): MeResponse {
        val resp = client.get("$base/api/auth/me") { headers.appendAll(reqHeaders()) }
        return parse(resp)
    }

    suspend fun logout() {
        runCatching {
            client.post("$base/api/auth/logout") { headers.appendAll(reqHeaders()) }
        }
    }

    suspend fun updateSettings(preferredFormat: String? = null, displayName: String? = null): SettingsResponse {
        val body = mutableMapOf<String, String>()
        preferredFormat?.let { body["preferredFormat"] = it }
        displayName?.let { body["displayName"] = it }
        val resp = client.put("$base/api/auth/settings") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return parse(resp)
    }

    /* ---------------- library ---------------- */

    suspend fun tracks(): TracksResponse = get("/api/library/tracks")
    suspend fun track(id: String): TrackResponse = get("/api/library/tracks/$id")
    suspend fun likeTrack(id: String): LikeResponse = post("/api/library/tracks/$id/like", emptyMap<String, String>())
    suspend fun albums(): AlbumsResponse = get("/api/library/albums")
    suspend fun albumDetail(id: String): AlbumDetailResponse = get("/api/library/albums/$id")
    suspend fun likeAlbum(id: String): LikeResponse = post("/api/library/albums/$id/like", emptyMap<String, String>())
    suspend fun artists(): ArtistsResponse = get("/api/library/artists")
    suspend fun artistDetail(id: String): ArtistDetailResponse = get("/api/library/artists/$id")
    suspend fun likeArtist(id: String): LikeResponse = post("/api/library/artists/$id/like", emptyMap<String, String>())

    suspend fun likedTracks(): TracksResponse = get("/api/library/liked/tracks")
    suspend fun likedAlbums(): AlbumsResponse = get("/api/library/liked/albums")
    suspend fun likedArtists(): ArtistsResponse = get("/api/library/liked/artists")

    suspend fun home(): HomeData = get("/api/library/home")
    suspend fun downloads(): DownloadsResponse = get("/api/library/downloads")
    suspend fun librarySearch(q: String): SearchResponse = get("/api/library/search", params = mapOf("q" to q))

    /* ---------------- discover ---------------- */

    suspend fun discoverSearch(q: String): DiscoverResult = get("/api/discover/search", params = mapOf("q" to q))
    suspend fun discoverArtist(mbid: String): DiscoverArtistDetail = get("/api/discover/artist/${encode(mbid)}")
    suspend fun discoverAlbum(mbid: String): DiscoverAlbumDetail = get("/api/discover/album/${encode(mbid)}")
    suspend fun discoverPlaylist(id: String): DiscoverPlaylistDetail = get("/api/discover/playlist/${encode(id)}")
    suspend fun discoverUser(id: String): DiscoverUserDetail = get("/api/discover/user/${encode(id)}")

    suspend fun discoverPlay(request: DiscoverPlayRequest): PlayResponse {
        val resp = client.post("$base/api/discover/play") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return parse(resp)
    }

    suspend fun discoverPlayMany(items: List<DiscoverPlayRequest>): PlayResponse {
        val resp = client.post("$base/api/discover/play-many") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(mapOf("items" to items))
        }
        return parse(resp)
    }

    /* ---------------- playlists ---------------- */

    suspend fun playlists(): PlaylistsResponse = get("/api/playlists")
    suspend fun createPlaylist(name: String, description: String = ""): PlaylistResponse {
        val resp = client.post("$base/api/playlists") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to name, "description" to description))
        }
        return parse(resp)
    }
    suspend fun playlist(id: String): PlaylistResponse = get("/api/playlists/$id")
    suspend fun renamePlaylist(id: String, name: String, description: String? = null): PlaylistResponse {
        val resp = client.patch("$base/api/playlists/$id") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to name, "description" to description))
        }
        return parse(resp)
    }
    suspend fun deletePlaylist(id: String) {
        val resp = client.delete("$base/api/playlists/$id") { headers.appendAll(reqHeaders()) }
        parse<PlaylistActionResponse>(resp)
    }
    suspend fun addTracksToPlaylist(id: String, trackIds: List<String>) {
        val resp = client.post("$base/api/playlists/$id/tracks") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(mapOf("trackIds" to trackIds))
        }
        parse<PlaylistActionResponse>(resp)
    }
    suspend fun removeTrackFromPlaylist(id: String, trackId: String) {
        val resp = client.delete("$base/api/playlists/$id/tracks/$trackId") { headers.appendAll(reqHeaders()) }
        parse<PlaylistActionResponse>(resp)
    }
    suspend fun reorderPlaylist(id: String, trackIds: List<String>) {
        val resp = client.post("$base/api/playlists/$id/reorder") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            setBody(mapOf("trackIds" to trackIds))
        }
        parse<PlaylistActionResponse>(resp)
    }

    /* ---------------- status ---------------- */

    suspend fun status(): Status = get("/api/status")
    suspend fun userCounts(): UserCounts = get("/api/status/user")

    /* ---------------- streaming ---------------- */

    /** Full URL for a track's stream, token included for ExoPlayer. */
    fun streamUrl(trackId: String): String {
        val t = token ?: return "$base/api/stream/$trackId"
        return "$base/api/stream/$trackId?t=${encode(t)}"
    }

    suspend fun downloadTrackToFile(trackId: String, target: java.io.File, onProgress: (Long, Long) -> Unit): String {
        return withContext(Dispatchers.IO) {
            val resp = client.get(streamUrl(trackId)) { headers.appendAll(reqHeaders()) }
            if (!resp.status.isSuccess()) {
                throw ApiException("Stream request failed (${resp.status.value})", resp.status.value)
            }
            val length = resp.contentLength() ?: 0L
            val channel = resp.bodyAsChannel()
            val out = java.io.FileOutputStream(target)
            try {
                var written = 0L
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    if (read == 0) {
                        if (!channel.awaitContent(1)) break
                        continue
                    }
                    out.write(buffer, 0, read)
                    written += read
                    onProgress(written, length)
                }
                out.flush()
            } finally {
                out.close()
            }
            resp.contentType()?.toString() ?: "application/octet-stream"
        }
    }

    /* ---------------- helpers ---------------- */

    private suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap()
    ): T {
        val url = if (params.isEmpty()) "$base$path" else "$base$path?" + params.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        val resp = client.get(url) { headers.appendAll(reqHeaders()) }
        return parse(resp)
    }

    private suspend inline fun <reified T> post(
        path: String,
        body: Any?
    ): T {
        val resp = client.post("$base$path") {
            headers.appendAll(reqHeaders())
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
        }
        return parse(resp)
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
