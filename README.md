# 🏆 World Cup 2026 Android Widget — Beautiful Edition

A Glance-based home-screen widget for Android with **enhanced visual design**
(gradient cards, glassmorphism accents, refined typography).

Shows live scores, next kick-off, and group standings for the
**2026 FIFA World Cup** (USA / Canada / Mexico, 11 June → 19 July 2026).

## ✨ What's new (vs. base)

- **Gradient live card** — magenta → crimson with subtle violet undertone (replaces flat red)
- **Gradient blue card** for the next kick-off (royal blue → midnight)
- **Slate-glass standings** with cyan/rose rank chips, gradient PTS pill for the leader
- **Bigger, crisper flags** (128×85, 3:2 ratio)
- **Accent bars** and **dividers** for visual hierarchy
- **Larger preview target** (3×3 cells) so the design has room to breathe
- All other functionality preserved: settings, work-manager refresh, EN/FR

## 🛠 Build & install

1. Install **Android Studio** → <https://developer.android.com/studio>
2. `File → Open` → select this folder (`wc2026_widget_beautiful/`)
3. Wait for Gradle sync to finish.
4. Connect your phone (USB-debugging on) and press **▶ Run**.
5. Long-press your home screen → **Widgets** → **World Cup 2026** → drop it.

> For APK only: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
> Output: `app/build/outputs/apk/debug/`

## ⚙️ Configure

Tap the widget footer ("Group & favourites ↗") to open the settings screen.
Pick any of the 12 groups (A → L) and pin your favourite team.

## 🌐 Data source

- `https://worldcup26.ir/get/teams` — 48 teams with flag, FIFA code, group
- `https://worldcup26.ir/get/games` — 104 fixtures, live scores, kick-off times
- `https://worldcup26.ir/get/groups` — group standings

No key, no auth, CORS-friendly public endpoint.

## 📦 Tech

- Kotlin 2.0 + Jetpack Compose & Glance (1.1.1)
- Retrofit 2 + Moshi + OkHttp
- Coil (in-widget image loading)
- WorkManager (15-min periodic refresh)
- minSdk 26, targetSdk 34
