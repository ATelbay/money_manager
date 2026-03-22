plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.core.ai"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.bundles.coroutines)
    implementation(libs.timber)
    implementation(projects.core.remoteconfig)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.testing)
}
