package io.github.sploonmc.builder

import io.github.sploonmc.builder.library.MavenDependency
import io.github.sploonmc.builder.piston.PistonAPI
import io.github.sploonmc.builder.piston.PistonAPI.getVersionMeta
import io.github.sploonmc.builder.piston.PistonLibrary
import io.sigpipe.jbsdiff.Patch
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.visitFileTree

private const val SPLOON_PATCHES_REPO_BASE_URL = "https://raw.githubusercontent.com/SploonMC/patches/refs/heads/master"

class SploonBuilder(val minecraftVersion: String, workDir: Path, val serverArgs: Array<String>) {
    private val pistonVersions = PistonAPI.getPistonVersions()
    private val versionMeta = pistonVersions.getVersionMeta(minecraftVersion)
    val bundlerDir = workDir.resolve("bundler")
    val vanillaServer = bundlerDir.resolve("mojang-server-$minecraftVersion.jar")
    val vanillaBundler = bundlerDir.resolve("mojang-bundler-$minecraftVersion.jar")
    val outputServer = bundlerDir.resolve("spigot-$minecraftVersion.jar")
    val patch = bundlerDir.resolve("$minecraftVersion.patch")

    init {
        librariesDir = workDir.resolve("libraries")
        setup()
    }

    private fun setup() {
        if (librariesDir.notExists()) librariesDir.createDirectories()

        download()
    }

    private fun download() {
        if (patch.notExists()) {
            println("Downloading patch...")
            downloadUri(URI("$SPLOON_PATCHES_REPO_BASE_URL/$minecraftVersion.patch"), patch)
            println("Patch downloaded!")
        }
    }

    fun verifyHashes(): Boolean {
        val patchedMeta = getUriJson<PatchedVersionMeta>(URI("$SPLOON_PATCHES_REPO_BASE_URL/$minecraftVersion.json"))
        val vanillaHash = vanillaServer.sha1()
        val patchedHash = outputServer.sha1()
        val patchHash = patch.sha1()
        val ok = vanillaHash == patchedMeta.vanillaJarHash
                && patchedHash == patchedMeta.patchedJarHash
                && patchHash == patchedMeta.patchHash

        if (!ok) {
            println("Failed verifying hashes:")

            println("Vanilla:")
            println("Found: $vanillaHash")
            println("Expected: ${patchedMeta.vanillaJarHash}")

            println("Patched:")
            println("Found: $patchedHash")
            println("Expected: ${patchedMeta.patchedJarHash}")

            println("Patch:")
            println("Found: $patchHash")
            println("Expected: ${patchedMeta.patchHash}")
        }

        return ok
    }

    @OptIn(ExperimentalPathApi::class)
    fun start() {
        val libs = getUri(URI("$SPLOON_PATCHES_REPO_BASE_URL/$minecraftVersion.libs"))
        val deps = MavenDependency.parseLines(libs.lines())
        val spigotJarPath = setupVanilla()
        deps.forEach { dep ->
            dep.download(false)
        }

        // Handle mojang deps to cover everything
        versionMeta.libraries.map(PistonLibrary::name).forEach { lib ->
            MavenDependency
                .parseLine(lib)
                .download(false)
                .map(Path::toUri)
                .map(URI::toURL)
        }

        val classpath = buildList {
            librariesDir.visitFileTree(object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    add(file.toUri().toURL())
                    return FileVisitResult.CONTINUE
                }
            })

            add(spigotJarPath.toUri().toURL())
        }

        val parentClassLoader = object {}.javaClass.classLoader.parent
        val classLoader = URLClassLoader(classpath.distinct().toTypedArray(), parentClassLoader)

        val serverThread = Thread({
            val mainClass = Class.forName("org.bukkit.craftbukkit.Main", true, classLoader)
            val mainHandle = MethodHandles.lookup()
                .findStatic(mainClass, "main", MethodType.methodType(Void.TYPE, Array<String>::class.java))
                .asFixedArity()

            mainHandle.invoke(serverArgs)
        }, "ServerMain")

        serverThread.contextClassLoader = classLoader
        serverThread.start()
    }

    fun setupVanilla(): Path {
        if (bundlerDir.notExists()) bundlerDir.createDirectory()

        if (vanillaBundler.notExists()) {
            println("Downloading vanilla...")
            downloadUri(URI(versionMeta.downloads.server.url), vanillaBundler)
            println("Vanilla downloaded!")
        }

        if (vanillaServer.notExists()) {
            val zip = JarFile(vanillaBundler.toFile())
            val entry = zip.getEntry("META-INF/versions/$minecraftVersion/server-$minecraftVersion.jar")
            zip.getInputStream(entry).use { input ->
                vanillaServer.outputStream().use { output ->
                    input.transferTo(output)
                }
            }

            zip.close()
        }

        if (outputServer.exists()) return outputServer

        println("Patching...")
        Patch.patch(vanillaServer.readBytes(), patch.readBytes(), outputServer.outputStream())
        println("Patched!")

        if (!verifyHashes()) {
            error("failed verifying hashes")
        }

        return outputServer
    }

    companion object {
        internal lateinit var librariesDir: Path
            private set
    }
}