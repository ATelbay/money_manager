plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.domain.exchangerate"
}

dependencies {
    implementation(projects.core.model)

    testImplementation(libs.bundles.testing)
}
