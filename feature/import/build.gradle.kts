plugins {
    alias(libs.plugins.moneymanager.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.feature.importstatement"
}

dependencies {
    implementation(projects.core.ai)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.model)
    implementation(projects.core.parser)
    implementation(projects.core.ui)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
