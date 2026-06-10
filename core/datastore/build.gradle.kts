plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    // Flow leaks through UserPreferences' public API -> expose coroutines transitively.
    api(libs.bundles.coroutines)
    implementation(libs.kotlinx.serialization.json)
}
