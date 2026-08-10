# AR Doom — Play Store Developer Setup Guide

This guide walks you through publishing AR Doom to the Google Play Store.

## Prerequisites

1. **Google Play Developer Account** — $25 one-time fee at https://play.google.com/console/signup
2. **Android Studio** — Latest version (Hedgehog 2023.1+)
3. **A signing keystore** — See below
4. **An ARCore-compatible device** for testing (Samsung Galaxy S25 recommended)

---

## Step 1: Create a Signing Keystore

You need a keystore to sign your release builds. **Keep this safe — you need the same key for all updates.**

```bash
keytool -genkey -v \
  -keystore ardoom-release.keystore \
  -alias ardoom \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Enter a strong password and fill in the identity fields when prompted.

---

## Step 2: Configure Signing in the Project

Option A — **local.properties** (recommended, gitignored):
```properties
ardoom.storeFile=/absolute/path/to/ardoom-release.keystore
ardoom.storePassword=your_store_password
ardoom.keyAlias=ardoom
ardoom.keyPassword=your_key_password
```

Option B — **Environment variables** (for CI/CD):
```bash
export ARDOOM_STORE_FILE=/path/to/ardoom-release.keystore
export ARDOOM_STORE_PASSWORD=your_store_password
export ARDOOM_KEY_ALIAS=ardoom
export ARDOOM_KEY_PASSWORD=your_key_password
```

For GitHub Actions CI, add these as repository secrets:
1. Go to your repo → Settings → Secrets and variables → Actions
2. Add: `ARDOOM_STORE_FILE` (base64-encoded keystore), `ARDOOM_STORE_PASSWORD`, `ARDOOM_KEY_ALIAS`, `ARDOOM_KEY_PASSWORD`
3. The release workflow uses these automatically on git tag pushes

---

## Step 3: Build the Release AAB

Google Play requires Android App Bundles (.aab), not APKs:

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

For a direct-install APK (for testing):
```bash
./gradlew assembleRelease
```

---

## Step 4: Create the Play Store Listing

1. Go to https://play.google.com/console
2. Click **Create app**
3. Fill in:
   - **App name:** AR Doom — Augmented Reality Shooter
   - **Package name:** com.ardoom
   - **Category:** Games > Action
   - **Free** (no cost)

---

## Step 5: Fill in Store Listing Details

Use `play-store/listing.md` for all the copy:

- **Short description** (80 chars)
- **Full description** (4000 chars)
- **App icon:** 512x512 PNG
- **Feature graphic:** 1024x500 PNG
- **Phone screenshots:** 2-8 screenshots (1080x1920 or 16:9)
- **App category:** Games > Action
- **Tags:** Shooter, AR, Action

---

## Step 6: Content Rating

1. Go to App content → Content rating
2. Fill out the IARC questionnaire:
   - Contains fantasy violence: **Yes**
   - Contains realistic violence: **No**
   - Contains blood/gore: **Yes** (mild)
   - Contains strong language: **No**
   - Target audience: **18+**
3. Result: **Mature 17+**

---

## Step 7: Privacy Policy

1. The privacy policy is in `app/src/main/assets/privacy_policy.html`
2. Host it online (GitHub Pages, your website, etc.)
3. Add the URL in: Play Console → App content → Privacy Policy
4. Example: `https://proctorpiffx.github.io/ar-doom-android/privacy-policy.html`

---

## Step 8: Upload the AAB

1. Go to Production → Create release
2. Upload `app-release.aab`
3. Add release notes (from `play-store/changelog.md`)
4. Click **Review release**
5. Click **Start rollout to production**

---

## Step 9: Wait for Review

- First review typically takes **1-7 days**
- You'll get an email when approved
- Check status in Play Console → Review status

---

## Step 10: Update App Data Safety

Play Console → App content → Data safety:
- Does your app collect data? **No**
- Does your app use camera? **Yes** (for AR, not stored)
- Does your app use device sensors? **Yes** (for AR tracking)

---

## Testing Before Release

1. **Internal testing** — Upload AAB → Create internal testing release → Add yourself as tester
2. **Closed testing** — Invite specific testers by email
3. **Open testing** — Public beta via Play Store listing

Test on your S25:
```bash
./gradlew installDebug
```

---

## Useful Commands

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK | `./gradlew assembleRelease` |
| Build release AAB | `./gradlew bundleRelease` |
| Install debug on device | `./gradlew installDebug` |
| Clean build | `./gradlew clean` |
| Lint check | `./gradlew lint` |
| Run unit tests | `./gradlew test` |

---

## GitHub Actions CI/CD

This project includes two workflows:

1. **build-apk.yml** — Runs on every push/PR to main. Builds debug + release APKs + AAB, uploads as artifacts.
2. **release.yml** — Runs on git tag push (`v*`). Builds signed release, creates GitHub Release with download links.

To trigger a release:
```bash
git tag v0.1.0
git push origin v0.1.0
```

The CI will build and create a GitHub Release automatically.
