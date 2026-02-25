plugins {
    alias(libs.plugins.moneymanager.android.feature)
}

android {
    namespace = "com.atelbay.money_manager.presentation.transactions"
}

dependencies {
    implementation(projects.domain.transactions)
    implementation(projects.domain.categories)
    implementation(projects.domain.accounts)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.core.datastore)
}
