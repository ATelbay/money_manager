plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.importstatement"
}

dependencies {
    implementation(projects.domain.`import`)
    implementation(projects.domain.accounts)
    implementation(projects.domain.auth)
    implementation(projects.domain.categories)
    implementation(projects.core.model)
    implementation(projects.core.auth)
    implementation(projects.core.remoteconfig)
    implementation(projects.core.ui)
    implementation(projects.core.datastore)

    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}
