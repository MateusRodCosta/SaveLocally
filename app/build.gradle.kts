import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.mateusrodcosta.apps.share2storage"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mateusrodcosta.apps.share2storage"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 28
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String

            // Always enable v2 and v3 signing schemes, which will be used on modern Android OSes
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    androidResources {
        @Suppress("UnstableApiUsage") generateLocaleConfig = true
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        // This is set to false starting with minSdk >= 28, but I want uncompressed DEX files with minSdk 26
        // According to https://developer.android.com/build/releases/past-releases/agp-4-2-0-release-notes#dex-files-uncompressed-in-apks-when-minsdk-=-28-or-higher:
        //
        // > This causes an increase in APK size, but it results in a smaller installation size on the device, and the download size is roughly the same.
        //
        // Currently this makes the APK ~1MB heavier
        //
        dex.useLegacyPackaging = false
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        // Requested by F-Droid (https://github.com/MateusRodCosta/Share2Storage/issues/44)
        // Disables dependency metadata when building APKs.
        includeInApk = false
    }
}

dependencies {
    implementation(libs.bundles.androidx.integration)
    implementation(libs.bundles.androidx.ktx)
    runtimeOnly(libs.coroutines)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.integration)
    debugImplementation(libs.compose.ui.tooling)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestRuntimeOnly(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.compose.ui.test.junit4)
}