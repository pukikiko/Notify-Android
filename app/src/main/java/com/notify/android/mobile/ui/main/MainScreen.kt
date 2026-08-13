package com.notify.android.mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notify.core.model.User
import com.notify.android.mobile.ui.artist.ArtistScreen
import com.notify.android.mobile.ui.album.AlbumScreen
import com.notify.android.mobile.ui.home.HomeScreen
import com.notify.android.mobile.ui.library.LibraryScreen
import com.notify.android.mobile.ui.library.LikedSongsScreen
import com.notify.android.mobile.ui.nowplaying.NowPlayingScreen
import com.notify.android.mobile.ui.offline.OfflineScreen
import com.notify.core.ui.player.PlayerViewModel
import com.notify.android.mobile.ui.playlist.PlaylistScreen
import com.notify.android.mobile.ui.search.SearchScreen
import com.notify.android.mobile.ui.settings.SettingsScreen
import com.notify.android.mobile.ui.theme.SpotifyBlack
import com.notify.android.mobile.ui.theme.SpotifySurfaceHigh
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SEARCH_QUERY = "search?q={q}"
    const val LIBRARY = "library"
    const val LIKED_SONGS = "liked"
    const val PLAYLIST = "playlist/{id}"
    const val ARTIST = "artist/{id}"
    const val ALBUM = "album/{id}"
    const val SETTINGS = "settings"
    const val OFFLINE = "offline"
    const val NOW_PLAYING = "nowplaying"
    const val CREATE = "create"

    fun search(query: String = ""): String =
        "search?q=" + URLEncoder.encode(query, "UTF-8")
    fun playlist(id: String) = "playlist/$id"
    fun artist(id: String) = "artist/$id"
    fun album(id: String) = "album/$id"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(user: User, playerVm: PlayerViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var showCreate by remember { mutableStateOf(false) }

    val onNowPlaying = currentDestination?.route == Routes.NOW_PLAYING

    Scaffold(
        containerColor = SpotifyBlack,
        bottomBar = {
            // Hide the mini player + bottom navigation while the full-screen
            // player is in the foreground, exactly like the official app.
            if (!onNowPlaying) {
                Column {
                    MiniPlayer(
                        playerVm = playerVm,
                        onOpen = {
                            navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true }
                        }
                    )
                    NotifyBottomBar(
                        currentRoute = currentDestination?.route,
                        onNavigate = { route ->
                            if (route == Routes.CREATE) {
                                showCreate = true
                                return@NotifyBottomBar
                            }
                            if (route == Routes.HOME) {
                                // Home must always land on Home. restoreState on the start
                                // destination can restore a stale saved stack (a "random
                                // tab"), so Home navigates without it.
                                navController.navigate(Routes.HOME) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    playerVm = playerVm,
                    onOpenArtist = { id -> navController.navigate(Routes.artist(id)) },
                    onOpenAlbum = { id -> navController.navigate(Routes.album(id)) },
                    onOpenPlaylist = { id -> navController.navigate(Routes.playlist(id)) },
                    onOpenSearch = { q -> navController.navigate(Routes.search(q)) },
                    onOpenLibrary = { navController.navigate(Routes.LIBRARY) }
                )
            }
            composable(
                Routes.SEARCH_QUERY,
                arguments = listOf(navArgument("q") { defaultValue = "" })
            ) { entry ->
                val q = entry.arguments?.getString("q") ?: ""
                SearchScreen(playerVm = playerVm, initialQuery = q,
                    onOpenArtist = { id -> navController.navigate(Routes.artist(id)) },
                    onOpenAlbum = { id -> navController.navigate(Routes.album(id)) },
                    onOpenPlaylist = { id -> navController.navigate(Routes.playlist(id)) })
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenLikedSongs = { navController.navigate(Routes.LIKED_SONGS) },
                    onOpenPlaylist = { id -> navController.navigate(Routes.playlist(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.LIKED_SONGS) {
                LikedSongsScreen(
                    playerVm = playerVm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PLAYLIST, arguments = listOf(navArgument("id") { defaultValue = "" })) { entry ->
                PlaylistScreen(
                    playlistId = entry.arguments?.getString("id") ?: "",
                    playerVm = playerVm,
                    onBack = { navController.popBackStack() },
                    onOpenArtist = { id -> navController.navigate(Routes.artist(id)) },
                    onOpenAlbum = { id -> navController.navigate(Routes.album(id)) }
                )
            }
            composable(Routes.ARTIST, arguments = listOf(navArgument("id") { defaultValue = "" })) { entry ->
                ArtistScreen(
                    artistId = entry.arguments?.getString("id") ?: "",
                    playerVm = playerVm,
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { id -> navController.navigate(Routes.album(id)) },
                    onOpenSearch = { q -> navController.navigate(Routes.search(q)) }
                )
            }
            composable(Routes.ALBUM, arguments = listOf(navArgument("id") { defaultValue = "" })) { entry ->
                AlbumScreen(
                    albumId = entry.arguments?.getString("id") ?: "",
                    playerVm = playerVm,
                    onBack = { navController.popBackStack() },
                    onOpenArtist = { id -> navController.navigate(Routes.artist(id)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenOffline = { navController.navigate(Routes.OFFLINE) },
                    onLogout = onLogout
                )
            }
            composable(Routes.OFFLINE) {
                OfflineScreen(
                    playerVm = playerVm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    playerVm = playerVm,
                    onBack = { navController.popBackStack() },
                    onOpenArtist = { id -> navController.navigate(Routes.artist(id)) },
                    onOpenAlbum = { id -> navController.navigate(Routes.album(id)) }
                )
            }
        }
    }

    if (showCreate) {
        ModalBottomSheet(
            onDismissRequest = { showCreate = false },
            containerColor = Color(0xFF282828),
            contentColor = Color.White
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Create",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                CreateSheetItem("New playlist") {
                    showCreate = false
                    navController.navigate(Routes.LIBRARY)
                }
            }
        }
    }
}

@Composable
private fun CreateSheetItem(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF4D4D4D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White)
        }
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun NotifyBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
        BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search),
        BottomNavItem(Routes.LIBRARY, "Your Library", Icons.Filled.LibraryMusic),
        BottomNavItem(Routes.CREATE, "Create", Icons.Filled.AddCircle)
    )
    NavigationBar(containerColor = Color(0xFF000000)) {
        items.forEach { item ->
            val selected = when (item.route) {
                Routes.SEARCH -> currentRoute == Routes.SEARCH || currentRoute?.startsWith("${Routes.SEARCH}?") == true
                else -> currentRoute == item.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color(0xFFB3B3B3),
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color(0xFFB3B3B3),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

/** Spotify-style floating rounded mini player above the bottom navigation. */
@Composable
fun MiniPlayer(playerVm: PlayerViewModel, onOpen: () -> Unit) {
    val current by playerVm.current.collectAsState()
    val playing by playerVm.playing.collectAsState()
    val position by playerVm.position.collectAsState()
    val duration by playerVm.duration.collectAsState()

    val track = current ?: return

    val artUrl = playerVm.currentArtUrl()
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SpotifySurfaceHigh)
            .clickable { onOpen() }
    ) {
        // thin progress line along the very top edge, Spotify-style
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopStart)
                .background(Color(0xFF4D4D4D))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(Color.White)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.notify.android.mobile.ui.components.Artwork(
                url = artUrl,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    track.displayArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { playerVm.toggle() }) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White
                )
            }
            IconButton(onClick = { playerVm.next() }) {
                Icon(Icons.Filled.SkipNext, "Next", tint = Color.White)
            }
        }
    }
}
