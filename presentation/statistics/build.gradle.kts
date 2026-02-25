plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.statistics"
}

dependencies {
    implementation(projects.domain.statistics)
    implementation(projects.core.model)
    implementation(projects.core.ui)
}
