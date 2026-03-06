plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.settings"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
    implementation(projects.domain.auth)
    implementation(projects.domain.exchangerate)
    implementation(projects.domain.transactions)

    testImplementation(libs.bundles.testing)
}
