plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.categories"
}

dependencies {
    implementation(projects.domain.categories)
    implementation(projects.core.model)
    implementation(projects.core.ui)
}
