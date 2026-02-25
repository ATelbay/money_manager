plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.categories"
}

dependencies {
    implementation(projects.domain.categories)
    implementation(projects.core.database)
    implementation(projects.core.model)
}
