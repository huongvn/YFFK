import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

val youtubeApiKey: String = localProps.getProperty("YOUTUBE_API_KEY") ?: ""

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
        for (i in 1..20) {
            val propName = if (i == 1) "YOUTUBE_PLAYLIST_ID" else "YOUTUBE_PLAYLIST_ID_$i"
            val value = localProps.getProperty(propName) ?: ""
            buildConfigField("String", "YOUTUBE_PLAYLIST_ID_$i", "\"$value\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false
        buildConfig = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
}