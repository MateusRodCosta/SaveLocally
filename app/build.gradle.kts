@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.mateusrodcosta.apps.share2storage"

    compileSdk = 34
    defaultConfig {
        applicationId = "com.mateusrodcosta.apps.share2storage"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 33
        versionCode = 13
        versionName = "0.9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    implementation(libs.androidx.activity.compose)

    testImplementation(libs.bundles.testing)
    androidTestImplementation(libs.bundles.ui.testing)
}