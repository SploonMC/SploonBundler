plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "io.github.sploonmc.builder"
version = "1.0.0-SNAPSHOT"

dependencies {
    implementation("com.github.codemonstur:simplexml:3.2.0")
    implementation("io.sigpipe:jbsdiff:1.0")
    implementation("org.sharegov:mjson:1.4.0")
}

tasks.shadowJar {
    archiveClassifier = ""
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    manifest {
        this.attributes["Main-Class"] = "io.github.sploonmc.builder.MainKt"
    }
}