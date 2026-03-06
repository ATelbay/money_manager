plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.auth"
}

dependencies {
    implementation(projects.domain.auth)
    implementation(projects.core.auth)
}
