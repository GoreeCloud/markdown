plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.goreecloud.markdown"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.goreecloud.markdown"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev.1"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "GoreeCloud Markdown DEV")
        }
        create("qa") {
            initWith(getByName("debug"))
            // Isolated package; does not modify existing Development app data.
            applicationIdSuffix = ".qa"
            resValue("string", "app_name", "GoreeCloud Markdown QA")
        }
        release {
            isMinifyEnabled = false
            resValue("string", "app_name", "GoreeCloud Markdown")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.noties.markwon:core:4.6.2")
    testImplementation("junit:junit:4.13.2")
}
