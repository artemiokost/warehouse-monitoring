plugins {
    application
}

dependencies {
    implementation(project(":common"))
    runtimeOnly(libs.logback.classic)
}

application {
    applicationDefaultJvmArgs = listOf("--sun-misc-unsafe-memory-access=allow")
    mainClass = "app.central.ApplicationKt"
}
