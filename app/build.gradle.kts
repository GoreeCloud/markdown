plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Default supports local QA builds; CI supplies a source-specific, isolated slot.
val qaSlot = providers.gradleProperty("goreecloudQaSlot").orNull ?: "qa"
require(qaSlot == "qa" || Regex("qa[a-f0-9]{10}").matches(qaSlot)) {
    "QA slot must be 'qa' or 'qa' followed by exactly ten lowercase hexadecimal characters"
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
            // A per-source CI slot avoids updating an earlier, differently signed QA APK.
            applicationIdSuffix = ".$qaSlot"
            resValue(
                "string", "app_name",
                if (qaSlot == "qa") "GoreeCloud Markdown QA"
                else "GoreeCloud Markdown QA ${qaSlot.removePrefix("qa")}",
            )
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
