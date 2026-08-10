# 📱 How to Run AR DOOM on Your Android Phone (No PC Needed)

## What You Need
- An Android phone with ARCore support (Samsung Galaxy S25 ✅)
- Android 7.0 or newer
- A rear camera
- ~15MB of free space

## Step-by-Step

### 1. Download the APK
Open your phone's browser and go to:

```
https://github.com/proctorpiffx/ar-doom-android/releases/tag/v0.1-alpha
```

Scroll down to **Assets** and tap **`AR-Doom-v0.1-alpha.apk`** to download it.

### 2. Allow Install from Unknown Sources (if prompted)
When you tap the downloaded file, your phone may block it with a message like *"For your security, your phone is not allowed to install unknown apps from this source."*

**To fix this:**
- Tap **Settings** on the popup
- Toggle **"Allow from this source"** to ON
- Go back and tap **Install**

> **On Galaxy S25:** Settings → Apps → Special access → Install unknown apps → [your browser] → Allow from this source

### 3. Install the APK
- Open the **Files** app (or **My Files** on Samsung)
- Go to **Downloads**
- Tap **AR-Doom-v0.1-alpha.apk**
- Tap **Install**
- Wait for it to finish (a few seconds)

### 4. Launch the Game
- Find **AR Doom** in your app drawer
- Tap to launch

### 5. Grant Camera Permission
The first time you open the game, it will ask for camera access:
- Tap **Allow** (or **While using the app**)
- The game **cannot run** without this — it needs the camera for AR

### 6. Let ARCore Initialize
When the game starts:
- You'll see your camera feed
- **Move your phone around slowly** — left, right, up, down
- ARCore needs to "see" the space to track your position
- This takes about 5–10 seconds
- You'll know it's ready when enemies start appearing

### 7. Play!
- **Tap anywhere on screen** to shoot
- Enemies spawn in waves — kill them all to advance
- Watch your **HEALTH** (top-left) — if it hits 0, game over
- Your **AMMO** (bottom-right) is limited per clip — it refills automatically
- **SCORE** increases with each kill
- Each wave gets harder with more enemies

### 8. Tips
- Hold your phone in **landscape** for the best view
- **Physically walk** closer to enemies for easier hits
- Look around — enemies can spawn behind you
- The Chaingun fires faster but burns ammo quicker
- Cacodemons and Barons take more hits — use the Shotgun up close

## Troubleshooting

**"ARCore is not supported"**
→ Your phone doesn't support ARCore. The S25 does, so make sure ARCore is installed (Google Play → search "Google Play Services for AR").

**Black screen after launch**
→ Move your phone around more. ARCore needs motion to initialize tracking. Make sure you're in a well-lit area.

**Game crashes on start**
→ Make sure no other camera app is running. Close any AR apps in the background.

**Can't shoot / nothing happens**
→ Wait for enemies to appear. They spawn 2 seconds after AR tracking starts. Tap directly on or near an enemy.

## Want to Build It Yourself?
See the main [README.md](../README.md) for building from source with Android Studio.
