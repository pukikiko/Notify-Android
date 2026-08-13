package com.notify.core.ui.navigation

import com.notify.core.model.Album
import com.notify.core.model.Artist
import com.notify.core.model.DiscoverAlbum
import com.notify.core.model.DiscoverArtist

/**
 * Resolves the navigation id for an album/artist entity. The backend provides
 * an `href` that already points at the right route ("/album/6" for a library
 * row, "/album/sp-<mbid>" for a Spotify release, "/album/catalog:..." for the
 * offline catalog). Falling back to the mbid (or raw id) mirrors the web app.
 */
fun Album.navId(): String =
    href?.substringAfterLast('/') ?: mbid?.let { "sp-$it" } ?: id

fun Artist.navId(): String =
    href?.substringAfterLast('/') ?: mbid?.let { "sp-$it" } ?: id

fun DiscoverAlbum.navId(): String =
    href?.substringAfterLast('/') ?: mbid?.let { "sp-$it" } ?: id ?: ""

fun DiscoverArtist.navId(): String =
    href?.substringAfterLast('/') ?: mbid?.let { "sp-$it" } ?: id ?: ""

fun String?.albumNavIdFallback(): String = this ?: ""
