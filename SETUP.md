         _        _          _          _       
        /\ \     /\ \       /\ \       /\ \     
       /  \ \    \ \ \     /  \ \     /  \ \    
      / /\ \ \   /\ \_\   / /\ \ \   / /\ \ \   
     / / /\ \_\ / /\/_/  / / /\ \_\ / / /\ \_\  
    / / /_/ / // / /    / /_/_ \/_// /_/_ \/_/  
   / / /__\/ // / /    / /____/\  / /____/\     
  / / /_____// / /    / /\____\/ / /\____\/     
 / / /   ___/ / /__  / / /      / / /           
/ / /   /\__\/_/___\/ / /      / / /            
\/_/    \/_________/\/_/       \/_/             
                                                

# AR Doom — First-Time Setup & Installation Guide

This guide covers everything from cloning the repo to playing the game on your S25.

---

## Part 1: Developer Setup

### Prerequisites

1. **Install Android Studio** (Hedgehog 2023.1 or newer)
   - Download: https://developer.android.com/studio
   - Make sure to install Android SDK 35, Build Tools 34.0.0, and NDK

2. **Install JDK 17**
   - Android Studio bundles a JDK, but if setting up CLI: install Temurin JDK 17
   - Verify: `java -version` should show 17.x

3. **Install Git**
   - Download: https://git-scm.com/downloads

4. **Samsung Galaxy S25** (or any ARCore-compatible device)
   - Check: https://developers.google.com/ar/devices
   - Enable Developer Options: Settings → About phone → tap "Build number" 7 times
   - Enable USB Debugging: Settings → Developer options → USB Debugging = ON

---

### Step 1: Clone the Repository

```bash
git clone https://github.com/proctorpiffx/ar-doom-android.git
cd ar-doom-android
```

### Step 2: Open in Android Studio

1. Launch Android Studio
2. Click **Open** (or File → Open)
3. Navigate to the `ar-doom-android` folder and select it
4. Wait for Gradle sync to complete (first time downloads dependencies — can take 5-10 minutes)
5. If prompted, install the recommended SDK components

### Step 3: Make the Gradle Wrapper Executable (Linux/Mac)

```bash
chmod +x gradlew
```

### Step 4: Build the Debug APK

**Option A — Android Studio:**
1. Click the green ▶️ Play button (or Run → Run 'app')
2. Select your connected S25
3. The app installs and launches

**Option B — Command Line:**
```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Step 5: Install on Your S25

**Via ADB:**
```bash
# Make sure USB debugging is enabled
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Via file transfer:**
1. Copy `app-debug.apk` to your S25
2. On the S25: open the file (may need to enable "Install from unknown sources")
3. Tap Install

---

## Part 2: First-Time Play Experience

### What Happens on First Launch

1. **Camera permission dialog** — Tap "Allow" (required for AR)
2. **ARCore install** (if not already installed)
   - The app checks if ARCore is installed
   - If not, it redirects you to the Play Store to install "Google Play Services for AR"
   - This is a ~20MB background service — install it and reopen AR Doom
3. **AR initialization**
   - The app starts the camera and begins tracking
   - You'll see your camera feed on screen
   - **Move your phone around slowly** to let ARCore map your space
   - This takes 2-5 seconds in good lighting
4. **Game starts**
   - Wave 1 banner appears
   - First enemies spawn on surfaces in front of you within 2 seconds

### Playing the Game

| Action | How |
|--------|-----|
| **Aim** | Physically point your phone at enemies |
| **Fire** | Tap anywhere on the screen |
| **Switch weapon** | Swipe left/right or long-press |
| **Get ammo** | Swipe down |
| **Move** | Physically walk in your space |
| **Look around** | Physically turn your body/phone |

### Tips for Best Experience

- **Play in a well-lit room** — ARCore needs light to track
- **Clear some space** — You'll be moving around
- **Hold phone in landscape** — The game is locked to landscape
- **Start in an open area** — Living rooms work great
- **Enemies spawn 2-5 meters away** — Give yourself room to back up
- **Listen for sounds** — (Once sound assets are added) enemies make noise before attacking

---

## Part 3: Building a Play Store Release

### Create a Keystore

```bash
keytool -genkey -v \
  -keystore ardoom-release.keystore \
  -alias ardoom \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

### Configure Signing

Create or edit `local.properties` in the project root (this file is gitignored):

```properties
sdk.dir=/path/to/Android/Sdk
ardoom.storeFile=/absolute/path/to/ardoom-release.keystore
ardoom.storePassword=your_store_password
ardoom.keyAlias=ardoom
ardoom.keyPassword=your_key_password
```

### Build the Release AAB

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Build Release APK (for direct distribution)

```bash
./gradlew assembleRelease
```

### Automated CI Builds

The project includes GitHub Actions workflows:

1. **Every push to main** builds debug + release APKs and AABs automatically
2. **On git tag** (e.g., `v0.1.0`) creates a GitHub Release with download links

```bash
# Tag a release
git tag v0.1.0
git push origin v0.1.0
# CI builds and creates a GitHub Release automatically
```

For signed CI builds, add keystore as GitHub repo secrets:
- `ARDOOM_STORE_FILE` (base64-encoded keystore file)
- `ARDOOM_STORE_PASSWORD`
- `ARDOOM_KEY_ALIAS`
- `ARDOOM_KEY_PASSWORD`

---

## Part 4: Publishing to Google Play

See `play-store/README.md` for the full step-by-step Play Console guide.

Quick summary:
1. Create a Play Console account ($25 one-time fee)
2. Create app → name: "AR Doom", package: `com.ardoom`
3. Upload the signed AAB
4. Fill in the store listing (use `play-store/listing.md`)
5. Complete the content rating questionnaire (Mature 17+)
6. Set up the privacy policy URL
7. Submit for review (typically 1-7 days)

---

## Part 5: Adding Real Game Assets

### DOOM Sprite Art

1. Place PNG sprite sheets in `app/src/main/assets/sprites/`
2. Update `SpriteTextureManager.kt` → `loadFromAssets()` method to load real textures
3. Each enemy type needs: idle, walk (2 frames), hurt, dying, dead frames

### Sound Effects

1. Place .wav or .ogg files in `app/src/main/res/raw/`
2. Naming: `sfx_pistol.wav`, `sfx_shotgun.wav`, `sfx_imp_growl.wav`, etc.
3. The AudioManager loads these automatically by name

### App Icon

1. Replace the default icon in `app/src/main/res/mipmap-*/ic_launcher.png`
2. Or use Android Studio's Image Asset Studio: right-click res → New → Image Asset
3. Required sizes: 48x48 (mdpi), 72x72 (hdpi), 96x96 (xhdpi), 144x144 (xxhdpi), 192x192 (xxxhdpi)
4. Play Store icon: 512x512 PNG

---

## Troubleshooting

### "ARCore not supported"
- Make sure your device is on the [supported list](https://developers.google.com/ar/devices)
- Update Google Play Services
- The S25 is fully supported

### "Camera permission denied"
- Go to Settings → Apps → AR Doom → Permissions → Camera → Allow

### "Black screen" / no AR tracking
- Make sure you're in a well-lit area
- Move your phone around slowly to let ARCore map the space
- Try restarting the app

### Build errors
- Run `./gradlew clean` then rebuild
- Make sure Android SDK 34 and 35 are installed
- Check JDK version: needs 17+
- Run `./gradlew --info` for detailed error messages

### Enemies not spawning
- Walk around to give ARCore more tracking data
- Make sure surfaces are visible (floors, walls, tables)
- Check that depth mode is working (Settings on S25)

---

## Quick Reference

| What | Command |
|------|---------|
| Clone | `git clone https://github.com/proctorpiffx/ar-doom-android.git` |
| Build debug | `./gradlew assembleDebug` |
| Install debug | `./gradlew installDebug` |
| Build release AAB | `./gradlew bundleRelease` |
| Build release APK | `./gradlew assembleRelease` |
| Clean | `./gradlew clean` |
| Run tests | `./gradlew test` |
| Lint | `./gradlew lint` |
| CI release | `git tag v0.1.0 && git push origin v0.1.0` |
