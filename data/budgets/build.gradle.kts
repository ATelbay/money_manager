plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.budgets"
}

dependencies {
    implementation(projects.domain.budgets)
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.data.sync)
    implementation(libs.room.ktx)
}
