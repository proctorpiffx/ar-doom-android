plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ardoom"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ardoom"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

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
            isMinifyEnabled = false
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
