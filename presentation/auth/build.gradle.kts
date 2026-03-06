plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.auth"
}

dependencies {
    implementation(projects.domain.auth)
    implementation(projects.core.auth)
    implementation(projects.core.ui)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
}
