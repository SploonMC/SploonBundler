package io.github.sploonmc.builder

import io.github.sploonmc.builder.library.MavenDependency
import io.sigpipe.jbsdiff.Patch
import mjson.Json
import org.apache.commons.compress.archivers.zip.ZipFile
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

// TODO move to constants
private const val SPLOON_PATCHES_URL = "https://raw.githubusercontent.com/SploonMC/patches/master/<version>.libs"

class SploonBuilder(val minecraftVersion: String, workDir: Path) {

    init {
        workDirectory = workDir
        setup()
    }

    private fun setup() {
        if (workDirectory.notExists()) workDirectory.createDirectory()
    }

    fun start() {
        val testFile = Path("C:\\Users\\immrt\\Downloads\\1.21.3.libs")
        val url = URI(SPLOON_PATCHES_URL.replace("<version>", minecraftVersion))
//      url.toURL().openStream().reader().readLines()
        val deps = testFile.inputStream().reader().use { reader ->
            MavenDependency.parseLines(reader.readLines())
        }.toMutableList()



        val spigotJarPath = setupVanilla()

        val classPath = deps.flatMap {
            it.download(false)
        }.map { it.toUri().toURL() }.toMutableList()

        // Handle mojang deps to cover everything
        // TODO remove hard code
        val json = Json.read(URI("https://piston-meta.mojang.com/v1/packages/fec76f40dbeb023db42fdf80d85624fc1f326d06/1.21.3.json").toURL())
        for (library in json.at("libraries").asJsonList()) {
            val gav = library.at("name").asString()
            classPath.addAll(MavenDependency.parseLine(gav).download(false).map {it.toUri().toURL()})
        }

        classPath.add(spigotJarPath.toUri().toURL())

        val parentClassLoader = object {}.javaClass.classLoader.parent
        val classLoader = URLClassLoader(classPath.toTypedArray(), parentClassLoader)

        val serverThread = Thread({
            val mainClass = Class.forName("org.bukkit.craftbukkit.Main", true, classLoader)
            val mainHandle = MethodHandles.lookup()
                .findStatic(mainClass, "main", MethodType.methodType(Void.TYPE, Array<String>::class.java))
                .asFixedArity()

            mainHandle.invoke(arrayOf<String>(""))
        }, "ServerMain")

        serverThread.contextClassLoader = classLoader
        serverThread.start()
    }

//    <X extends Throwable> RuntimeException sneakyThrow(final Throwable ex) throws X {
//        throw (X) ex;
//    }

    fun setupVanilla(): Path {
        val vanillaServerDir = workDirectory.resolve("bundler")
        if (vanillaServerDir.notExists()) vanillaServerDir.createDirectory()

        val vanillaBundler = vanillaServerDir.resolve("mojang-bundler-$minecraftVersion.jar")
        if (vanillaBundler.notExists()) {
            // TODO non hardcoded
            vanillaBundler.createFile()
            vanillaBundler.outputStream().use { output ->
                URI("https://piston-data.mojang.com/v1/objects/45810d238246d90e811d896f87b14695b7fb6839/server.jar").toURL()
                    .openStream().transferTo(output)
            }
        }
        val vanillaServer = vanillaServerDir.resolve("mojang-server-$minecraftVersion.jar")
        if (vanillaServer.notExists()) {
            val zip = ZipFile(vanillaBundler.toFile())
            val entry = zip.getEntry("META-INF/versions/$minecraftVersion/server-$minecraftVersion.jar")
            zip.getInputStream(entry).use { input ->
                vanillaServer.outputStream().use { output ->
                    input.transferTo(output)
                }
            }
        }

        val outputServer = vanillaServerDir.resolve("spigot-$minecraftVersion.jar")
        // TODO check hash from sploon or smth
//        if (outputServer.exists()) return outputServer
        // TODO input of path not hard code
        val patch = Path("C:\\Users\\immrt\\Downloads\\1.21.3.patch")
        Patch.patch(vanillaServer.readBytes(), patch.readBytes(), outputServer.outputStream())

        return outputServer
    }


    companion object {
        internal lateinit var workDirectory: Path
            private set
    }
}