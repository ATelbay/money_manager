import java.util.Properties

plugins {
    alias(libs.plugins.moneymanager.android.application)
    alias(libs.plugins.moneymanager.android.compose)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services)
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
}

dependencies {
    // Modules
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
    implementation(projects.core.common)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.transactions)
    implementation(projects.feature.categories)
    implementation(projects.feature.statistics)
    implementation(projects.feature.accounts)
    implementation(projects.core.ai)
    implementation(projects.feature.settings)
    implementation(projects.feature.`import`)

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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
}
