package io.github.sploonmc.bundler.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun transform(inputJar: Path, outputJar: Path, transformations: Map<String, (ClassVisitor) -> ClassVisitor>) {
    ZipInputStream(Files.newInputStream(inputJar)).use { zipIn ->
        ZipOutputStream(Files.newOutputStream(outputJar)).use { zipOut ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    zipOut.putNextEntry(entry)
                    zipOut.closeEntry()
                    entry = zipIn.nextEntry
                    continue
                }

                zipOut.putNextEntry(entry)
                var readBytes = zipIn.readBytes()
                if (entry.name.endsWith(".class")) {
                    val transformation = transformations[entry.name]
                    if (transformation != null) {
                        val reader = ClassReader(readBytes)
                        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                        reader.accept(transformation(writer), ClassReader.EXPAND_FRAMES or ClassReader.SKIP_FRAMES)
                        readBytes = writer.toByteArray()
                    }
                }

                zipOut.write(readBytes)
                zipOut.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
}
