plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.domain.budgets"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.domain.transactions)
}
