# Prism Music 🎵

An elegant, modern, and highly-optimized local music player for Android, designed with a pure AMOLED dark aesthetic, frosted matte glassmorphism, dynamic color harmony, and rich audio management features.

---

## ✨ Features

- **AMOLED Matte Glassmorphic UI**: Pure deep black (`#000000`) theme with subtle frosted glass surfaces, borderless cards, and dynamic accent colors extracted in real-time from the active album cover art.
- **Media3 & ExoPlayer Audio Engine**: Robust background playback service with seamless notification controls, lock screen media session integration, and reliable playback persistence.
- **Synchronized Two-Cover Gesture Swiping**: Swipe left or right on the Now Playing screen to fluidly slide adjacent album art in real-time with continuous finger tracking.
- **Custom Lock Screen Activity**: Dedicated sleek lock screen interface during active playback with slide-to-dismiss gesture and a long-press touch-lock to prevent accidental pocket touches.
- **Comprehensive Embedded & Adjacent Lyrics**:
  - Automatically extracts embedded lyrics from ID3v2 frames (`USLT`, `SYLT`, `ULT`), Vorbis comments (`LYRICS` / `UNSYNCEDLYRICS` in FLAC/OGG), and MP4 metadata atoms (`©lyr`).
  - Reads external `.lrc` and `.txt` files adjacent to the music tracks.
  - On-the-fly lazy loading upon playback—no full library rescan required.
- **Independent Tab Layouts**: Customize view modes independently for every tab (Songs, Folders, Artists, Playlists, Favorites, Dusty Tracks):
  - Normal List
  - Compact List
  - 3-Column Grid
  - 4-Column Grid
- **Customizable Bottom Navigation Bar**:
  - Reorder navigation tabs via long-press and drag.
  - Toggle visibility of any of the 7 tabs in Settings with automatic horizontal weight rebalancing.
  - Inactive tabs display clean, enlarged icons; the active tab displays an icon with a bold title.
- **Floating Auto-Hiding Scrollbars**: Sleek floating scrollbar thumb tinted with dynamic track accents across all tabs with fast-touch dragging and auto-fadeout when stationary.
- **Bottom-Up Categorized Search**: Fast search bar pinned at the bottom above the MiniPlayer with categorized, expandable results (Songs, Artists, Albums).
- **Audio Equalizer & Sound Effects**: 10-band graphic equalizer with customizable presets, Bass Boost, and Audio Virtualizer.
- **Built-in ID3 Tag Editor**: Edit track titles, artist names, album details, genres, lyrics, and embed custom album art directly within the app.
- **M3U / M3U8 Playlist Support**: Import local playlists or use the one-tap "Auto-Scan All Playlists" feature to find and import all `.m3u`/`.m3u8` playlists on the device.
- **Customizable Home Screen Widget**: Resizable 1:1 square cover widget with playback controls, 10-touch segment seekbar, and adjustable opacity down to 100% transparent.
- **Safe Pull-to-Refresh**: Guarded against accidental flings—requires the list to reach the ceiling first before triggering a scan.

---

## 🛠 Tech Stack

- **Platform**: Android (Min SDK 26, Target SDK 34)
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose & Material 3
- **Audio Core**: AndroidX Media3 (ExoPlayer 1.4.1)
- **Database & Storage**: Room Database 2.6.1 with KSP
- **Image Loading**: Coil 2.7.0
- **Color Extraction**: AndroidX Palette
- **Build System**: Gradle 8.7 (Kotlin DSL) with Android Gradle Plugin 8.6.1

---

## 🚀 Building & Running

### Prerequisites
- [Android Studio Ladybug (or newer)](https://developer.android.com/studio)
- JDK 17
- Android SDK (API 34)

### Clone & Build
```bash
git clone git@github.com:A-Magpie/PrismMusic.git
cd PrismMusic
```

Run debug build:
```bash
./gradlew assembleDebug
```

---

## 📄 License
This project is open-source and distributed under the [MIT License](LICENSE).
