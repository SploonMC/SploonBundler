package io.github.sploonmc.builder

import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
//    MavenDependency("org.spigotmc", "spigot-api", "1.21.3-R0.1-SNAPSHOT").download()

//    println(MavenDependency.latestSnapshotVersion("https://hub.spigotmc.org/nexus/repository/snapshots/org/spigotmc/spigot-api/1.19.1-R0.1-SNAPSHOT/maven-metadata.xml"))

    if (args.isEmpty()) {
        println("Version argument not passed")
        exitProcess(1)
    }

    SploonBuilder(args[0], Path("work")).start()
}