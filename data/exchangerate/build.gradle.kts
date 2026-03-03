plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.exchangerate"
}

dependencies {
    implementation(projects.domain.exchangerate)
    implementation(projects.core.datastore)

    implementation(libs.bundles.coroutines)

    testImplementation(libs.bundles.testing)
}
