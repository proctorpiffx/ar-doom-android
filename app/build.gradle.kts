plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ardoom"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ardoom"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // ─── Release signing ──────────────────────────────────────────
    // For Play Store: create a keystore and reference it via local.properties
    // or environment variables so secrets never go in git.
    //
    // Create keystore:
    //   keytool -genkey -v -keystore ardoom-release.keystore \
    //     -alias ardoom -keyalg RSA -keysize 2048 -validity 10000
    //
    // Then in local.properties (gitignored):
    //   ardoom.storeFile=/path/to/ardoom-release.keystore
    //   ardoom.storePassword=yourpassword
    //   ardoom.keyAlias=ardoom
    //   ardoom.keyPassword=yourpassword
    //
    // Or set env vars: ARDOOM_STORE_FILE, ARDOOM_STORE_PASSWORD,
    //                  ARDOOM_KEY_ALIAS, ARDOOM_KEY_PASSWORD
    // ──────────────────────────────────────────────────────────────

    val storeFilePath = (project.findProperty("ardoom.storeFile") as String?)
        ?: System.getenv("ARDOOM_STORE_FILE")
    val storePassword = (project.findProperty("ardoom.storePassword") as String?)
        ?: System.getenv("ARDOOM_STORE_PASSWORD")
    val keyAlias = (project.findProperty("ardoom.keyAlias") as String?)
        ?: System.getenv("ARDOOM_KEY_ALIAS")
    val keyPassword = (project.findProperty("ardoom.keyPassword") as String?)
        ?: System.getenv("ARDOOM_KEY_PASSWORD")

    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (storeFilePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    // Play Store: generate AAB (App Bundle) by default
    // Run: ./gradlew bundleRelease
    // Or APK: ./gradlew assembleRelease
    bundle {
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // ARCore
    implementation(libs.arcore)

    // JSON
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
