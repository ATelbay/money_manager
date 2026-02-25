plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.domain.importstatement"
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.core.ai)
    implementation(projects.core.parser)
    implementation(projects.core.common)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)
}
