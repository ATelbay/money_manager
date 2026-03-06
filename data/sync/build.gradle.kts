plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.sync"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.database)
    implementation(projects.core.firestore)
    implementation(libs.bundles.coroutines)
    implementation(libs.timber)
}
