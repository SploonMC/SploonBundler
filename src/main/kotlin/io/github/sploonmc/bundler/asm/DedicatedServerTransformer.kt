package io.github.sploonmc.bundler.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter

object DedicatedServerTransformer : ClassTransformer(::DedicatedServerClassVisitor) {
    const val TARGET_ZIP_ENTRY = "net/minecraft/server/dedicated/DedicatedServer.class"
    const val TARGET_CLASS = "net/minecraft/server/dedicated/DedicatedServer"
    const val LOGGER_CLASS = "org/apache/logging/log4j/core/Logger"
    const val LOGGER_REMOVE_APPENDER_METHOD = "removeAppender"

    override fun filterZipEntry(zipEntryName: String) = zipEntryName == TARGET_ZIP_ENTRY

    class DedicatedServerClassVisitor(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
            if (name == TARGET_CLASS) {
                super.visit(version, access, name, signature, superName, interfaces)
            } else {
                cv.visit(version, access, name, signature, superName, interfaces)
            }
        }

        override fun visitMethod(
            access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
            return DedicatedServerMethodVisitor(api, methodVisitor, access, name, descriptor)
        }
    }

    class DedicatedServerMethodVisitor(
        api: Int, methodVisitor: MethodVisitor, access: Int, name: String, descriptor: String
    ) : AdviceAdapter(api, methodVisitor, access, name, descriptor) {
        override fun visitMethodInsn(
            opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean
        ) {
            if (opcode == INVOKEVIRTUAL && owner == LOGGER_CLASS && name == LOGGER_REMOVE_APPENDER_METHOD) return

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }
}
