plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.onboarding"
}

dependencies {
    implementation(projects.domain.accounts)
    implementation(projects.domain.auth)
    implementation(projects.domain.exchangerate)
    implementation(projects.domain.sync)
    implementation(projects.core.auth)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.datastore)
}
