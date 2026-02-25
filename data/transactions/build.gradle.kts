plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.data.transactions"
}

dependencies {
    implementation(projects.domain.transactions)
    implementation(projects.core.database)
    implementation(projects.core.model)
}
