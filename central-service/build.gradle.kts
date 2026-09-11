plugins {
    application
}

dependencies {
    implementation(project(":protocol"))
    implementation(libs.bundles.central)
    runtimeOnly(libs.logback.classic)
}

application {
    applicationDefaultJvmArgs = listOf("--sun-misc-unsafe-memory-access=allow")
    mainClass = "app.central.ApplicationKt"
}
