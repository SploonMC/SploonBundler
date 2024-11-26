package io.github.sploonmc.bundler.asm.transformations

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class DedicatedServerTransformer(parent: ClassVisitor) : BasicClassTransformer(parent) {
    companion object {
        const val TARGET = "net/minecraft/server/dedicated/DedicatedServer.class"
    }

    override val targetClass: String = TARGET

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return DedicatedServerMethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions))
    }

}

private class DedicatedServerMethodTransformer(visitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, visitor) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (opcode == Opcodes.INVOKEVIRTUAL && name.equals("removeAppender")) {
            super.visitInsn(Opcodes.POP2)
            return
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}
