# Prism Music 🎵

A modern, AMOLED dark-themed local music player for Android, built with Jetpack Compose and Media3 (ExoPlayer).

---

## ✨ Key Features

- **AMOLED Glassmorphic UI**: Pure black theme with frosted glass accents and real-time dynamic palette extraction from album art.
- **Robust Audio Engine**: Powered by AndroidX Media3 with background playback, lock screen controls, and custom playback notification.
- **Interactive Gestures**: Two-cover slide gesture tracking on Now Playing and custom lock-screen overlay with accidental-touch lock.
- **Lyrics Support**: Embedded ID3/Vorbis/MP4 tags + external `.lrc`/`.txt` lyrics with in-place font scaling and editing.
- **Flexible Layouts**: List, Compact, and Grid (3 or 4 columns) independently configured per tab.
- **Library & Audio Tools**: Built-in 10-band equalizer, ID3 tag editor, M3U/M3U8 auto-import, storage deletion, and home screen widget.
- **Customizable Navigation**: Reorder or hide tabs with dynamic auto-layout.

---

## 🛠 Tech Stack

- **Kotlin** & **Jetpack Compose** (Material 3)
- **AndroidX Media3** (ExoPlayer)
- **Room Database** & **Coil**
- **AndroidX Palette**

---

## 🚀 Building from Source

```bash
git clone git@github.com:A-Magpie/PrismMusic.git
cd PrismMusic
./gradlew assembleDebug
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
