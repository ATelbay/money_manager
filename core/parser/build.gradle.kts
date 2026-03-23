plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.core.parser"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.remoteconfig)

    implementation(libs.pdfbox.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.pdfbox)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.testing)
}
