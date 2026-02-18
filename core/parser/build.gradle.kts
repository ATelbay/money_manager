plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.core.parser"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.remoteconfig)

    implementation(libs.pdfbox.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}
