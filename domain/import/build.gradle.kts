plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.atelbay.money_manager.domain.importstatement"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "HMAC_KEY",
            "\"${findProperty("hmac.key") ?: "money_manager_candidate_v1"}\""
        )
    }
}

dependencies {
    implementation(projects.core.database)
    implementation(libs.room.ktx)
    implementation(projects.core.model)
    implementation(projects.core.ai)
    implementation(projects.core.parser)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.firestore)
    implementation(projects.core.remoteconfig)
    implementation(projects.domain.categories)
    implementation(projects.domain.auth)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.coroutines)
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}
