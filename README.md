# Museify

**Your music, your discovery.**

Museify is a modern Android music discovery and streaming application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **MVVM** architecture, powered by the **Jamendo API**.

---

## Features

- **Home Discovery Dashboard:** Museify branding, time-of-day greeting, search entry point, Featured Tracks carousel, Recently Played tracks, Recommended for You, and Curated Discovery cards.
- **Debounced Music Search:** Real-time Jamendo search by song title or artist with 400 ms debouncing, loading, empty, and retryable error states.
- **Audio Streaming & Mini Player:** Powered by **AndroidX Media3 ExoPlayer** with play, pause, resume, real-time seek slider, previous/next queue navigation, unavailable-track handling, and a persistent synchronized Mini Player.
- **Song Details & Creative Commons Attribution:** Displays album artwork, artist, album, duration, Jamendo source, Creative Commons license summary, attribution details, and external Jamendo/license links.
- **Persistent Favorites:** Saved locally with **Room Database** so favorite tracks survive app restarts, with instant heart-toggle synchronization across screens.
- **Adaptive Light & Dark Mode (Not Night / Night):** Supports **System Default**, **Light Mode**, and **Dark Mode** using Museify's official 8-color brand palette with live switching that preserves playback and navigation state.
- **Bilingual Localization (Multi-Bahasa):** Supports **English** (`values/strings.xml`) and **Bahasa Indonesia** (`values-in/strings.xml`), defaulting to the device language with live in-app switching in Settings.
- **Responsive Design:** Adapts cleanly across small phones, large phones, tablets (`NavigationRail` + 2-column grids), and portrait/landscape orientations (two-pane `NowPlayingScreen` and `SongDetailsScreen`).

---

## Official Brand Palette

| Color | Hex |
|---|---|
| Midnight Navy | `#131B2B` |
| Deep Slate Blue | `#2F4A63` |
| Ocean Blue | `#5B91A3` |
| Soft Mint | `#CFEAE0` |
| Deep Plum | `#321E48` |
| Muted Steel Blue | `#43637E` |
| Electric Aqua | `#65DCD5` |
| Ice Mint | `#D9FFF4` |

---

## Tech Stack & Architecture

- **Language:** Kotlin (`2.0.21`)
- **UI & Design System:** Jetpack Compose + Material 3
- **Architecture:** MVVM with `StateFlow` and Kotlin Coroutines
- **Networking:** Retrofit `2.11.0` + Gson Converter (`https://api.jamendo.com/v3.0/`)
- **Image Loading:** Coil Compose `2.7.0`
- **Media Playback:** AndroidX Media3 ExoPlayer `1.5.1`
- **Local Storage:** Room `2.6.1` (KSP) for Favorites, DataStore Preferences `1.1.1` for Settings
- **Navigation:** Navigation Compose `2.8.5`

```text
com.biancaputri.museify
├── data/
│   ├── api/            # JamendoApi, DTOs, RetrofitInstance
│   ├── local/          # MuseifyDatabase, FavoriteDao, FavoriteEntity
│   ├── model/          # Song domain model
│   ├── preferences/    # SettingsDataStore (ThemeMode & AppLanguage)
│   └── repository/     # MusicRepository, FavoritesRepository
├── navigation/         # MuseifyNavHost (BottomBar / NavigationRail + routes)
├── player/             # MusicPlayerManager (shared Media3 ExoPlayer)
├── ui/
│   ├── components/     # SongCard, MiniPlayer, SearchBar, ArtworkImage, LoadingState
│   ├── screens/        # Home, Search, Favorites, NowPlaying, SongDetails, Settings
│   ├── theme/          # Color, Theme, Type, Localization
│   └── util/           # AdaptiveLayout, Formatters
└── viewmodel/          # MusicViewModel, SettingsViewModel
```

---

## Setup & Running Locally

1. Clone the repository and open it in **Android Studio**.
2. Obtain a free Client ID from [Jamendo Developer](https://developer.jamendo.com/).
3. Copy `secrets.properties.example` to `secrets.properties` in the project root (or add the entry to `local.properties`):
   ```properties
   JAMENDO_CLIENT_ID=your_jamendo_client_id_here
   ```
   *(Do not wrap the value in quotes. Both `secrets.properties` and `local.properties` are gitignored.)*
4. Click **Sync Project with Gradle Files** and run the `app` configuration on an Android device or emulator (API 24–36) with internet access.

---

## Verification

Run the JVM unit test suite and build debug APK:

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

## Music Source & Licensing

All music metadata, album artwork, and audio streams are provided by **Jamendo** (`https://api.jamendo.com/v3.0/`) under track-specific Creative Commons and Jamendo streaming terms.
