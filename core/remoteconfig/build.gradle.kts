plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.core.remoteconfig"
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.firestore)
    implementation(projects.core.model)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
