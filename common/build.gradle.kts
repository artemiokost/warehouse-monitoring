plugins {
    `java-library`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.bundles.common)
    api(libs.kotlinx.serialization.json)
}
