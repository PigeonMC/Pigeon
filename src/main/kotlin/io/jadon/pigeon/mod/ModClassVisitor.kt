package io.jadon.pigeon.mod

import io.jadon.pigeon.api.Mod
import io.jadon.pigeon.api.ModInfo
import jdk.internal.org.objectweb.asm.Type
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

class ModClassVisitor : ClassVisitor(Opcodes.ASM6) {

    companion object {

        val MOD_ANNOTATION = Type.getType(Mod::class.java).descriptor

        fun getModInfo(clazz: ByteArray): ModInfo? {
            val classReader = ClassReader(clazz)
            val modClassVisitor = ModClassVisitor()
            classReader.accept(modClassVisitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            val modInfo: ModInfo? = modClassVisitor.annotationVisitor?.modInfo
            // only return the modInfo if these is a valid id
            return if (modInfo?.id?.equals("null")?.not() == true) modInfo else null
        }

    }

    var className: String? = null
    internal var annotationVisitor: ModAnnotationVisitor? = null

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        className = name
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        return if (desc == MOD_ANNOTATION && visible) {
            annotationVisitor = ModAnnotationVisitor(className!!)
            annotationVisitor!!
        } else {
            super.visitAnnotation(desc, visible)
        }
    }

}

internal class ModAnnotationVisitor(className: String) : AnnotationVisitor(Opcodes.ASM6) {

    val modInfo = ModInfo("null", className.replace('/', '.'))

    override fun visit(name: String?, value: Any?) {
        when (name) {
            "id" -> {
                modInfo.id = value.toString()
            }
            else -> super.visit(name, value)
        }
    }

}
