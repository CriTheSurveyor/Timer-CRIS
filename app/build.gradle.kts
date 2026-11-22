plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cri.tabata"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cri.tabata"
        minSdk = 26
        targetSdk = 35
        // CI run number = build number, compared against the latest GitHub release to offer updates
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "build $versionCode"
    }
    // same key on every CI build, so updates install over the previous version; without the secret, the default debug key is used
    System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotEmpty() }?.let { pw ->
        signingConfigs.getByName("debug") {
            storeFile = file("timer.p12")
            storePassword = pw
            keyAlias = "timer"
            keyPassword = pw
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    testImplementation("junit:junit:4.13.2")
}
