plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.core.crypto"
}

dependencies {
    implementation(libs.tink.android)

    testImplementation(libs.bundles.testing)
}
