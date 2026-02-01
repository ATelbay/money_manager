plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.feature.statistics"
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.core.ui)
}
