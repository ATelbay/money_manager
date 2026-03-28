plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.budgets"
}

dependencies {
    implementation(projects.domain.budgets)
    implementation(projects.domain.categories)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.core.common)
}
