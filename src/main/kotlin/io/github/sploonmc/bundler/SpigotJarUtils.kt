package io.github.sploonmc.bundler

import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipInputStream
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText

private const val JAR_VERSIONS_PATH = "META-INF/versions"
private const val JAR_VERSIONS_LIST_PATH = "META-INF/versions.list"

fun JarFile.needsExtraction() = getJarEntry(JAR_VERSIONS_PATH) != null

fun extractJar(jarFile: Path, outputDirectory: Path) {
    if (outputDirectory.notExists()) {
        outputDirectory.createDirectories()
    }

    FileInputStream(jarFile.toFile()).use { fileStream ->
        ZipInputStream(fileStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                val outputPath = outputDirectory.resolve(entry.name)

                if (entry.isDirectory) {
                    outputPath.createDirectories()
                } else {
                    outputPath.parent.createDirectories()

                    FileOutputStream(outputPath.toFile()).use { fileOutStream ->
                        zipStream.copyTo(fileOutStream)
                    }
                }

                entry = zipStream.nextEntry
            }
        }
    }
}

fun extractServer(jarFile: Path, extractDir: Path, target: Path) {
    extractJar(jarFile, extractDir)

    val extractionPath = extractDir.resolve(JAR_VERSIONS_PATH)
    val versionsFilePath = extractDir.resolve(JAR_VERSIONS_LIST_PATH)
    val content = runCatching(versionsFilePath::readText).getOrNull()

    val jar = if (content == null) {
        println("Failed reading versions.list. Using $jarFile instead.")
        jarFile
    } else {
        extractionPath.resolve(content.split("\t")[2])
    }

    if (target.notExists()) {
        target.parent.createDirectories()
    }

    jar.copyTo(target)
}