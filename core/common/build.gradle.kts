plugins {
    alias(libs.plugins.moneymanager.android.library)
}

android {
    namespace = "com.atelbay.money_manager.core.common"
}

dependencies {
    implementation(libs.bundles.coroutines)
    implementation(libs.timber)
}
