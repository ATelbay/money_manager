plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.recurring"
}

dependencies {
    implementation(projects.domain.recurring)
    implementation(projects.data.sync)
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(libs.room.ktx)
}
