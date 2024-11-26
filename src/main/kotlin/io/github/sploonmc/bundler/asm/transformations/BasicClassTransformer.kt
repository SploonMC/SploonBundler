package io.github.sploonmc.bundler.asm.transformations

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

abstract class BasicClassTransformer(parent: ClassVisitor) : ClassVisitor(Opcodes.ASM9, parent) {
    abstract val targetClass: String

    override fun visitEnd() {
        println("Finished transformation on $targetClass")
    }
}
