plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.accounts"
}

dependencies {
    implementation(projects.domain.accounts)
    implementation(projects.core.database)
    implementation(projects.core.model)
}
