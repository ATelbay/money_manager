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
        // HMAC key for hashing user ids in shared parser candidates. Provide via -Phmac.key=...
        // or the HMAC_KEY env var. A dev-only fallback is used for debug builds; release builds
        // MUST supply a real secret (enforced below) so the key never ships baked into the repo.
        val hmacKey = (findProperty("hmac.key") as String?)
            ?: System.getenv("HMAC_KEY")
            ?: "money_manager_candidate_dev_only"
        buildConfigField("String", "HMAC_KEY", "\"$hmacKey\"")
    }
}

// Refuse to assemble/bundle a release without a real HMAC key, instead of silently shipping the
// public dev fallback (which would make user-id hashes in parser_candidates trivially correlatable).
gradle.taskGraph.whenReady {
    val isReleaseBuild = allTasks.any { task ->
        task.name.contains("Release") &&
            (task.name.startsWith("assemble") || task.name.startsWith("bundle"))
    }
    val providedKey = (findProperty("hmac.key") as String?) ?: System.getenv("HMAC_KEY")
    if (isReleaseBuild && providedKey.isNullOrBlank()) {
        throw GradleException(
            "hmac.key (or HMAC_KEY env) must be set for release builds — refusing to ship the " +
                "public dev fallback key.",
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
