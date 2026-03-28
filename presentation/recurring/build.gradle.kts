plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.recurring"
}

dependencies {
    implementation(projects.domain.recurring)
    implementation(projects.domain.categories)
    implementation(projects.domain.accounts)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.core.common)
}
