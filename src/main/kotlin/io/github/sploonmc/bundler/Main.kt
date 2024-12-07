package io.github.sploonmc.bundler

import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val versionResource = readResource("/META-INF/sploon.version")

    if (args.isEmpty() && versionResource == null) {
        println("Version argument not passed")
        exitProcess(1)
    }

    val version = versionResource ?: args[0]

    val serverArgs = args.drop(1).toMutableList()
    if ("nogui" !in args) serverArgs.addFirst("nogui")

    SploonBundler(version, Path(""), serverArgs.toTypedArray()).start()
}
