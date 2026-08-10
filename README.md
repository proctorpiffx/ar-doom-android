# AR DOOM — Android AR Game

> Rip and tear, in real life.

An augmented reality DOOM game for Android that uses your phone's camera to project DOOM enemies into the real world. Physically move around, aim your phone, and blast imps in your living room.

## Target Device
- **Samsung Galaxy S25** (primary target)
- Any ARCore-compatible Android device (Android 10+ / API 29+)

## How It Works

1. **ARCore** tracks your position in real space using the S25's advanced sensors
2. **Enemies spawn** on detected surfaces in front of you (floors, walls, tables)
3. **Your phone is the weapon** — tap to fire, physically look around to aim
4. **Enemies chase you** through real space — back up, dodge, and fight
5. **Wave-based survival** — each wave brings more and tougher enemies

## Tech Stack

| Component | Technology |
|-----------|-----------|
| AR Engine | ARCore 1.42 |
| Rendering | OpenGL ES 3.0 |
| Language | Kotlin 2.0 |
| Build | Gradle 8.7 + AGP 8.5 |
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |
| Architecture | arm64-v8a (S25) |

## Project Structure

```
ar-doom-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── sprites/          # DOOM sprite sheets (PNG)
│       ├── java/com/ardoom/
│       │   ├── ARDoomApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── ar/
│       │   │   └── ARCameraManager.kt    # ARCore session + frame management
│       │   ├── game/
│       │   │   ├── GameEngine.kt          # Core game loop, combat, waves
│       │   │   └── Enemy.kt              # Enemy AI + entity types
│       │   ├── rendering/
│       │   │   ├── DoomRenderer.kt       # Main GL renderer
│       │   │   ├── BackgroundShader.kt   # AR camera feed background
│       │   │   ├── SpriteShader.kt       # Billboarded enemy sprites
│       │   │   ├── SpriteTextureManager.kt # Texture loading/management
│       │   │   └── EffectShader.kt       # Muzzle flash, projectiles, effects
│       │   ├── input/
│       │   │   └── TouchController.kt     # Touch gestures for combat
│       │   └── audio/
│       │       └── AudioManager.kt        # DOOM sound effects
│       └── res/
│           ├── layout/activity_main.xml    # HUD overlay + GL surface
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── raw/                       # Sound files (.wav/.ogg)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── .gitignore
```

## Controls

| Action | Input |
|--------|-------|
| Fire weapon | Tap screen |
| Switch weapon | Long press or swipe left/right |
| Reload/pickup | Swipe down |
| Move | Physically walk in real space |
| Look around | Physically turn/tilt phone |

## Enemy Types

| Enemy | HP | Speed | Damage | Score |
|-------|----|-------|--------|-------|
| Soldier | 30 | Slow | 10 | 50 |
| Imp | 60 | Fast | 20 | 100 |
| Demon (Pinky) | 150 | Medium | 40 | 250 |
| Cacodemon | 200 | Medium | 35 | 400 |
| Baron of Hell | 300 | Slow | 50 | 500 |

## Weapons

| Weapon | Damage | Fire Rate |
|--------|--------|-----------|
| Pistol | 25 | 300ms |
| Shotgun | 60 | 800ms |
| Chaingun | 20 | 100ms |
| Plasma Rifle | 80 | 500ms |
| BFG | 500 | 1500ms |

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- Android SDK 35
- An ARCore-compatible device (Samsung Galaxy S25 recommended)
- ARCore must be installed on device (Play Store auto-installs for supported devices)

### Build
```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/ar-doom-android.git
cd ar-doom-android

# Open in Android Studio or build from CLI
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Adding Real DOOM Assets
1. Place sprite sheets in `app/src/main/assets/sprites/`
2. Place sound effects in `app/src/main/res/raw/` (as `.wav` or `.ogg`)
3. Update `SpriteTextureManager.kt` to load actual PNG textures instead of generated placeholders

## Roadmap

- [x] ARCore camera + tracking
- [x] Enemy spawning on surfaces
- [x] Enemy AI (chase, attack, death)
- [x] Combat system (fire, damage, health)
- [x] Wave progression
- [x] HUD overlay
- [ ] Real DOOM sprite art integration
- [ ] DOOM sound effects
- [ ] Multiple weapon switching UI
- [ ] Health/ammo pickups spawning in AR
- [ ] Boss enemies
- [ ] Score persistence (leaderboard)
- [ ] Multiplayer (shared AR session)
- [ ] DOOM MIDI music playback

## License

MIT — see [LICENSE](LICENSE) file.

## Disclaimer

This is a fan project inspired by DOOM (id Software, 1993). DOOM is a trademark of id Software / Bethesda. This project is not affiliated with or endorsed by id Software. All original game assets are copyright their respective owners.

---

**RIP AND TEAR.**
