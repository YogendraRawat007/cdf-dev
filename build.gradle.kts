plugins {
    kotlin("jvm") version "1.9.22"
    id("org.graalvm.buildtools.native") version "0.10.1"
    application
}

group = "com.cognite"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("com.cognite.cdfdev.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.cognite.cdfdev.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("cdf-dev")
            mainClass.set("com.cognite.cdfdev.MainKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=kotlin")
            buildArgs.add("--initialize-at-run-time=io.netty")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}
