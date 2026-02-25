plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.onboarding"
}

dependencies {
    implementation(projects.domain.accounts)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.core.datastore)
}
