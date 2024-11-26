package io.github.sploonmc.bundler.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

abstract class ClassTransformer(val classVisitor: (ClassVisitor) -> ClassVisitor) {
    abstract fun filterZipEntry(zipEntryName: String): Boolean

    fun transform(inputJar: Path, outputJar: Path) {
        ZipInputStream(FileInputStream(inputJar.toFile())).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputJar.toFile())).use { zipOut ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (entry.isDirectory) {
                        zipOut.putNextEntry(entry)
                        zipOut.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    val entryName = entry.name
                    val newEntry = ZipEntry(entryName)

                    if (!filterZipEntry(newEntry.name)) {
                        entry = zipIn.nextEntry
                        continue
                    }

                    zipOut.putNextEntry(newEntry)

                    val classBytes = zipIn.readBytes()
                    val classReader = ClassReader(classBytes)
                    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
                    val cv = classVisitor(classWriter)

                    classReader.accept(cv, ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

                    zipOut.write(classWriter.toByteArray())

                    zipOut.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }
    }
}