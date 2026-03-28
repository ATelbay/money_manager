plugins {
    alias(libs.plugins.moneymanager.android.library)
    alias(libs.plugins.moneymanager.android.hilt)
}

android {
    namespace = "com.atelbay.money_manager.domain.recurring"
}

dependencies {
    implementation(projects.core.model)
implementation(projects.domain.transactions)
    implementation(projects.domain.accounts)
}
