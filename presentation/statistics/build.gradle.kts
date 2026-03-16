plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.statistics"
}

dependencies {
    implementation(projects.domain.statistics)
    implementation(projects.domain.transactions)
    implementation(projects.domain.categories)
    implementation(projects.domain.accounts)
    implementation(projects.domain.exchangerate)
    implementation(projects.core.datastore)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.bundles.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
}
