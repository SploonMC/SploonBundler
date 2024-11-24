package io.github.sploonmc.builder.library

import io.github.sploonmc.builder.SploonBuilder
import xmlparser.XmlParser
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.outputStream

data class MavenDependency(val groupId: String, val artifactId: String, val version: String) {

    fun download(override: Boolean) = download(this, override)

    override fun toString(): String {
        return "MavenDependency(groupId='$groupId', artifactId='$artifactId', version='$version')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MavenDependency

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false

        return true
    }


    companion object {
        private fun download(dependency: MavenDependency, override: Boolean): List<Path> {
            val output =
                SploonBuilder.workDirectory.resolve(dependency.groupId.replace(".", "/")).resolve(dependency.artifactId)
                    .resolve(dependency.version).resolve("${dependency.artifactId}-${dependency.version}.jar")

            if (output.exists() && !override) return listOf(output)

            val gav =
                "${dependency.groupId.replace(".", "/")}/${dependency.artifactId}/${dependency.version}/"

            val repository = Repository.parseRepository(dependency.groupId)

            var version = dependency.version
            if (dependency.version.contains("SNAPSHOT"))
                version = latestSnapshotVersion(repository.url + gav + "maven-metadata.xml")

            val suffix = gav + "${dependency.artifactId}-$version.jar"
            val url = URI(repository.url + suffix)

            val outputList = mutableListOf<Path>()
            outputList.addAll(locateTransitiveDeps(dependency, repository, version).flatMap { it.download(false) }.toMutableList())

            output.toFile().parentFile.mkdirs()
            output.outputStream().buffered().use { output ->
                url.toURL().openStream().buffered().use { input ->
                    input.transferTo(output)
                }
            }

            println("Downloaded '${dependency.artifactId}' with version: '${dependency.version}'")
            outputList.add(0, output)
            return outputList
        }

        fun parseLines(input: List<String>): List<MavenDependency> {
            return input.map { parseLine(it) }
        }

        fun parseLine(input: String): MavenDependency {
            val split = input.split(":")
            if (split.size < 3) error("Invalid GAV input: '$input'. Must have group:artifact:version")

            return MavenDependency(split[0], split[1], split[2])
        }

        private fun latestSnapshotVersion(metaDataUrl: String): String {
            val parser = XmlParser()
            val element = URI(metaDataUrl).toURL().openStream().reader().use { reader ->
                parser.fromXml(reader.readText())
            }

            return element.getElementsByTagName("versioning")[0].getElementsByTagName("snapshotVersion")[0].getElementsByTagName(
                "value"
            )[0].text
        }

        private fun locateTransitiveDeps(parent: MavenDependency, repository: Repository, version: String): List<MavenDependency> {
            val gavWithPom = "${parent.groupId.replace(".", "/")}/${parent.artifactId}/${parent.version}/${parent.artifactId}-$version.pom"

            val parser = XmlParser()
            val url = URI(repository.url + gavWithPom).toURL()
//            println(url)
            val pom = url.openStream().reader().use { reader ->
                parser.fromXml(reader.readText())
            }

            val dependencies = mutableListOf<MavenDependency>()

            val dependenciesArray = pom.getElementsByTagName("dependencies")
            if (dependenciesArray.isEmpty()) return dependencies
            for (element in dependenciesArray[0].getElementsByTagName("dependency")) {
                val scope = element.getElementsByTagName("scope")
                if (scope.isEmpty()) continue
                if (scope[0].text != "compile") continue

                val group = element.getElementsByTagName("groupId")[0].text
                val artifact = element.getElementsByTagName("artifactId")[0].text
                val versionArray = element.getElementsByTagName("version")
                if (versionArray.isEmpty()) continue
                val version = versionArray[0].text
                if (version.contains("\${")) continue

                dependencies.add(MavenDependency(group, artifact, version))
            }

            return dependencies
        }
    }
}

private enum class Repository(val url: String) {
    MOJANG("https://libraries.minecraft.net/"),
    SPIGOT("https://hub.spigotmc.org/nexus/repository/snapshots/"),
    CENTRAL("https://repo1.maven.org/maven2/");

    companion object {
        fun parseRepository(group: String): Repository {
            if (group.endsWith("mojang")) return MOJANG
            if (group.endsWith("spigotmc")) return SPIGOT
            return CENTRAL
        }
    }
}