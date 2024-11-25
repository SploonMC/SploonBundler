package io.github.sploonmc.bundler

import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Version argument not passed")
        exitProcess(1)
    }

    val serverArgs = args.drop(1).toMutableList()
    if ("nogui" !in args) serverArgs.addFirst("nogui")

    SploonBuilder(args[0], Path(""), serverArgs.toTypedArray()).start()
}