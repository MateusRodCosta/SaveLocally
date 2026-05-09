import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.koin.compiler)
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
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mateusrodcosta.apps.share2storage"
        minSdk = 28
        targetSdk = 36
        versionCode = 32
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // Enable v3 signing only, also on debug builds
            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
        }

        if (keystorePropertiesFile.exists()) {
            create("release") {
                if (keystorePropertiesFile.exists()) {
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                    storeFile = file(keystoreProperties["storeFile"] as String)
                    storePassword = keystoreProperties["storePassword"] as String
                }

                // Enable v3 signing only, which will be used on modern Android (9+)
                enableV1Signing = false
                enableV2Signing = false
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
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

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(libs.bundles.androidx.integration)
    implementation(libs.bundles.androidx.ktx)
    runtimeOnly(libs.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.integration)
    debugImplementation(libs.compose.ui.tooling)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    androidTestRuntimeOnly(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.compose.ui.test.junit4)

    implementation(project(":domain"))
    implementation(project(":data"))
}
