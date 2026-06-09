plugins {
    alias(libs.plugins.moneymanager.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // LocalDate leaks through ParsedTransaction's public API -> expose datetime transitively.
    api(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
