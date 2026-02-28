import java.util.Properties

plugins {
    alias(libs.plugins.moneymanager.android.application)
    alias(libs.plugins.moneymanager.android.compose)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.roborazzi)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingProp(fileKey: String, envKey: String): String? =
    keystoreProperties.getProperty(fileKey) ?: System.getenv(envKey)

android {
    namespace = "com.atelbay.money_manager"

    defaultConfig {
        applicationId = "com.atelbay.money_manager"
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(signingProp("storeFile", "SIGNING_STORE_FILE") ?: "keystore.jks")
            storePassword = signingProp("storePassword", "SIGNING_STORE_PASSWORD")
            keyAlias = signingProp("keyAlias", "SIGNING_KEY_ALIAS")
            keyPassword = signingProp("keyPassword", "SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Data (for Hilt DI wiring)
    implementation(projects.data.transactions)
    implementation(projects.data.categories)
    implementation(projects.data.accounts)

    // Presentation
    implementation(projects.presentation.onboarding)
    implementation(projects.presentation.transactions)
    implementation(projects.presentation.categories)
    implementation(projects.presentation.statistics)
    implementation(projects.presentation.accounts)
    implementation(projects.presentation.settings)
    implementation(projects.presentation.`import`)

    // Core
    implementation(projects.core.datastore)
    implementation(projects.core.ui)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.timber)

    // Debug
    debugImplementation(libs.leakcanary)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
}
