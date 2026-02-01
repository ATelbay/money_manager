plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.feature.settings"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
}
