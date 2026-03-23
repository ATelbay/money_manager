plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.core.model"
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
}
