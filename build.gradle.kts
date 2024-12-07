import java.io.FileOutputStream
import java.util.jar.*

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "io.github.sploonmc.bundler"
version = "0.0.0"

dependencies {
    implementation("com.github.codemonstur:simplexml:3.2.0")
    implementation("io.sigpipe:jbsdiff:1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.ow2.asm:asm:9.7.1")
}

tasks {
    shadowJar {
        archiveClassifier = ""
        version = ""
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        manifest {
            attributes["Main-Class"] = "io.github.sploonmc.bundler.MainKt"
        }
    }

    abstract class VersionTask : DefaultTask() {
        @get:Input
        abstract val version: Property<String>

        init {
            group = "bundler-versioning"
            description = "Creates a jar for a specific Minecraft version: $version"
        }

        @TaskAction
        fun createVersionedJar() {
            val version = version.get()

            val jarFile = project.layout.buildDirectory.file("libs/${project.name}.jar").get().asFile
            val outputFile = jarFile.parentFile.resolve("${project.name}-$version.jar")
            outputFile.delete()

            logger.lifecycle("Creating versioned jar: ${outputFile.absolutePath}")

            JarFile(jarFile).use { jar ->
                JarOutputStream(FileOutputStream(outputFile)).use { output ->
                    jar.entries().asSequence().forEach { entry ->
                        output.putNextEntry(JarEntry(entry.name))
                        jar.getInputStream(entry).use { it.copyTo(output) }
                    }

                    output.putNextEntry(JarEntry("META-INF/sploon.version"))
                    version.byteInputStream().use { it.copyTo(output) }
                }
            }

            logger.lifecycle("Versioned jar created at: ${outputFile.absolutePath}")
        }
    }

    fun versionTask(version: String) = register<VersionTask>(version.replace(".", "_")) {
        dependsOn(shadowJar)
        this.version = version
    }

    versionTask("1.21.3")
    versionTask("1.21.1")
}
