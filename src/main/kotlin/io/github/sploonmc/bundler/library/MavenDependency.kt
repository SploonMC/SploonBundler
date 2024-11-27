package io.github.sploonmc.bundler.library

import io.github.sploonmc.bundler.SploonBundler
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

    fun rewrite(): MavenDependency {
        var result = this

        LibraryRewriters.REWRITERS.forEach { rewriter ->
            if (!rewriter.isApplicable(result)) return@forEach
            result = rewriter.rewrite(result)
        }

        return result
    }

    companion object {
        private fun download(dependency: MavenDependency, override: Boolean): List<Path> {
            val output =
                SploonBundler.librariesDir.resolve(dependency.groupId.replace(".", "/")).resolve(dependency.artifactId)
                    .resolve(dependency.version).resolve("${dependency.artifactId}-${dependency.version}.jar")

            if (output.exists() && !override) return listOf(output)

            val gav =
                "${dependency.groupId.replace(".", "/")}/${dependency.artifactId}/${dependency.version}/"

            val repository = Repository.parseRepository(dependency)

            var version = dependency.version
            if (dependency.version.contains("SNAPSHOT"))
                version = latestSnapshotVersion(repository.url + gav + "maven-metadata.xml")

            val suffix = gav + "${dependency.artifactId}-$version.jar"
            var url = URI(repository.url + suffix)

            val isBungeeChatNonSnapshot = dependency.artifactId == "bungeecord-chat" && !dependency.version.endsWith("-SNAPSHOT")

            if (isBungeeChatNonSnapshot) {
                val metaUrl =
                    repository.url + "${dependency.groupId.replace(".", "/")}/${dependency.artifactId}/${dependency.version}-SNAPSHOT/maven-metadata.xml"

                val latestSnapshot = latestSnapshotVersion(metaUrl)
                url = URI(repository.url + "${dependency.groupId.replace(".", "/")}/${dependency.artifactId}/$version-SNAPSHOT/${dependency.artifactId}-$latestSnapshot.jar")
            }

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
            val isBungeeChatNonSnapshot = parent.artifactId == "bungeecord-chat" && !parent.version.endsWith("-SNAPSHOT")
            var gavWithPom = "${parent.groupId.replace(".", "/")}/${parent.artifactId}/${parent.version}/${parent.artifactId}-$version.pom"

            if (isBungeeChatNonSnapshot) {
                val metaUrl =
                    repository.url + "${parent.groupId.replace(".", "/")}/${parent.artifactId}/${parent.version}-SNAPSHOT/maven-metadata.xml"
                val latestSnapshot = latestSnapshotVersion(metaUrl)
                gavWithPom = "${parent.groupId.replace(".", "/")}/${parent.artifactId}/$version-SNAPSHOT/${parent.artifactId}-$latestSnapshot.pom"
            }

            val parser = XmlParser()
            val url = URI(repository.url + gavWithPom).toURL()
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

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}

private enum class Repository(val url: String) {
    MOJANG("https://libraries.minecraft.net/"),
    SPIGOT_SNAPSHOTS("https://hub.spigotmc.org/nexus/repository/snapshots/"),
    SPIGOT("https://hub.spigotmc.org/nexus/repository/public/"),
    CENTRAL("https://repo1.maven.org/maven2/"),
    VELOCITY("https://nexus.velocitypowered.com/repository/maven-public/");

    companion object {
        fun parseRepository(dependency: MavenDependency): Repository {
            if (dependency.groupId.endsWith("mojang")
                || dependency.artifactId.endsWith("mojang")
                || dependency.groupId.endsWith("lwjgl")
                && dependency.artifactId != "lwjgl-platform"
                ) return MOJANG
            if (dependency.artifactId == "lwjgl-platform" && "nightly" in dependency.version) return MOJANG
            if (dependency.groupId.endsWith("md-5")) return SPIGOT
            if (dependency.groupId.endsWith("spigotmc")) return SPIGOT_SNAPSHOTS
            if (dependency.groupId.endsWith("paulscode")) return VELOCITY
            return CENTRAL
        }
    }
}