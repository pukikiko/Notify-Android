# Notify for Android

> **Disclaimer:** This is **very early alpha-stage software** with **many, many
> bugs**. Expect crashes, broken features, and rough edges everywhere. Nothing
> here is stable - don't rely on it for anything important, and don't be
> surprised when things go wrong. Use at your own risk.

Native Android client for [Notify](https://github.com/notify/Notify) - your
self-hosted Spotify alternative. Two apps built from the same Kotlin / Jetpack
Compose core:

- **`app`** - phone/tablet app (Spotify mobile style)
- **`tv`** - Android TV app (Compose for TV, remote / D-pad navigation)

Both are functionally a copy of the Spotify experience for their platform and
cover every Notify feature: search (Spotify/Soulseek/YouTube Music/SoundCloud),
library, likes, playlists, artist/album pages, and full playback controls.

The mobile app adds one feature the web app doesn't have: **offline listening**.

## Features

- **Instance switching** - add any number of self-hosted Notify servers
  (`https://notify.mfc.pw`, a LAN box, your Docker instance…) and switch
  between them. Each instance keeps its own login.
- **Accounts** - login/register per instance.
- **Home** - greeting, popular artists/albums/songs, recently added, in-flight
  downloads.
- **Search** - "Browse all" genre grid plus Spotify discover results (artists,
  albums, playlists, songs) merged with your cached library.
- **Library** - liked songs / albums / artists.
- **Playlists** - create, rename, delete, add/remove tracks.
- **Artist / Album pages** - full Spotify tracklists and discography; playing
  something not cached yet resolves + downloads it automatically (live-streams
  while it downloads, Spotify-style).
- **Player** - queue, shuffle, repeat (off/all/one), seek, volume; Now Playing
  screen on mobile, full-screen overlay on TV.
- **Settings** - streaming format (server-side transcode), music sources,
  shared cache stats, account stats, instance management.
- **Offline (mobile)** - download any track for offline playback; a full
  offline library screen, plays with no network at all.
- **Premium** - teaser screen (premium plans are a placeholder for now).

## Architecture

```
core/   data + player (shared by both apps)
  model/            Kotlin models for every Notify API entity
  data/             Ktor client, session/instance store, offline store,
                    offline download manager, auth'd media data source
  player/           ExoPlayer engine (queue, shuffle, repeat, live downloads)
  di/               manual DI container (AppContainer)
  ui/               shared ViewModels + factories
app/    mobile UI (Compose Material3)
tv/     TV UI (Compose, D-pad focus)
```

- Networking: Ktor (OkHttp engine) + kotlinx.serialization
- Playback: Media3 / ExoPlayer with a data source that injects the instance
  auth token; offline tracks play straight from local files
- Persistence: DataStore (instances/sessions + offline index)

## Install with Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) installs the APK straight
from this repo's GitHub releases and keeps it updated. Tap the button for your
device - it opens Obtainium with the source, the APK filter and the app name
already filled in.

Phones and tablets

[![Add Notify to Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%7B%22id%22%3A%22com.notify.android%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fpukikiko%2FNotify-Android%22%2C%22author%22%3A%22pukikiko%22%2C%22name%22%3A%22Notify%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22notify-mobile-%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Afalse%7D%22%7D)

Android TV / Google TV

[![Add Notify TV to Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%7B%22id%22%3A%22com.notify.android.tv%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fpukikiko%2FNotify-Android%22%2C%22author%22%3A%22pukikiko%22%2C%22name%22%3A%22Notify%20TV%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22notify-tv-%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Afalse%7D%22%7D)

The buttons go through `apps.obtainium.imranr.dev`, which bounces to the
`obtainium://` link. GitHub strips custom URL schemes out of README links, so
it cannot be linked directly; the redirect page is part of the Obtainium
project and carries the config in the URL rather than storing anything. If your
browser refuses the hand-off, turn on "Allow forward URL requests to external
intents" (or similar) in its settings.

To add it by hand instead - on a TV, where there is no convenient browser:

1. **Add App** → URL: `https://github.com/pukikiko/Notify-Android`
2. Set **Filter APKs by Regular Expression** to the app you want:
   - `notify-mobile-` for phones and tablets
   - `notify-tv-` for Android TV / Google TV
3. Leave the rest at the defaults and hit **Add**.

Every release carries both apps, and they use different application IDs
(`com.notify.android` and `com.notify.android.tv`), so both can be installed
on the same device at the same time.

## Building

Requires JDK 17+ and the Android SDK (compileSdk 35).

```bash
./gradlew :app:assembleRelease   # mobile APK
./gradlew :tv:assembleRelease    # TV APK
```

Release builds are **minified and shrunk with R8**. The shared ProGuard rules
in `proguard-rules.pro` are mostly kotlinx.serialization keep rules - without
them R8 strips the generated serializer classes and the app crashes the moment
it tries to decode a server response.

APKs land in `app/build/outputs/apk/release/app-release.apk` and
`tv/build/outputs/apk/release/tv-release.apk`.

**Signing.** Release builds are signed from environment variables (or Gradle
properties) so no keystore ever needs to be committed:

| Env variable      | Value                   |
|-------------------|-------------------------|
| `KEYSTORE_PATH`   | Path to the `.jks` file |
| `KEYSTORE_PASSWORD` | Keystore password     |
| `KEY_ALIAS`       | Signing key alias       |
| `KEY_PASSWORD`    | Signing key password    |

If any of these are missing, the release build is produced **unsigned** (it
will not install). The CI workflow rebuilds the keystore in `keystore/` from a
base64 secret, so set these repository secrets:

- `KEYSTORE_BASE64` - `base64 -w0 < keystore.jks` (Linux/CI) or
  `base64 < keystore.jks | tr -d '\n'` (macOS)
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

Because the signing key never changes, an Obtainium update installs over the
previous build. Swapping in a different key would force everyone who installed
the old build to uninstall first.

## Releasing

Releases are what Obtainium sees, so they come from a tag:

```bash
git tag v1.2.3 && git push origin v1.2.3
```

`.github/workflows/release.yml` then builds both apps, checks each APK's
signature and version, and publishes `notify-mobile-1.2.3.apk` and
`notify-tv-1.2.3.apk` to a GitHub release named `v1.2.3`. Running the workflow
manually with a version instead creates the tag for you, and a tag such as
`v1.2.3-rc.1` is published as a pre-release (which Obtainium ignores unless
"Include prereleases" is on).

Versions must be `X.Y.Z`. The tag drives `versionName` and a `versionCode` of
`major * 1000000 + minor * 1000 + patch`, so tags have to keep climbing -
Android refuses to install an APK whose `versionCode` is lower than the
installed one. Local and PR builds have no tag and report version `0.0.0`.

## Using

1. Install the APK on your device/TV.
2. Open the app → **Add instance** → enter your server URL.
3. Log in (or create an account on the instance).
4. Search, hit play - Notify downloads and streams automatically.

On mobile, tap the download icon on the Now Playing screen to save a track for
offline; manage downloads in **Settings → Offline**.
