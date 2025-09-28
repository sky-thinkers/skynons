plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ktor) apply false
}

group = "com.skythinkers.skynons"
version = "0.0.1"

tasks.register<Exec>("setupBackendCmakeProject") {
    group = "build"
    description = "Setup backend cmake build"

    workingDir("backend")
    commandLine("cmake", "-DCMAKE_BUILD_TYPE=Release", "-Bbuild")
}

tasks.register<Exec>("compileBackend") {
    group = "build"
    description = "Build backend"
    dependsOn(tasks["setupBackendCmakeProject"])

    workingDir("backend")
    commandLine("cmake", "--build", "build", "-j8")
}
