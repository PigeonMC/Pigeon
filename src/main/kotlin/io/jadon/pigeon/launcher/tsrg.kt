package io.jadon.pigeon.launcher

import java.io.File

object TSrgUtil {

    // these classes are using data based on the TSRG format, not the SRG format

    private data class Clazz(val obf: String, val deobf: String,
                             val fields: MutableList<Field> = mutableListOf(),
                             val methods: MutableList<Method> = mutableListOf()) {
        override fun toString(): String = "$obf $deobf"
    }

    private data class Field(val obf: String, val deobf: String) {
        override fun toString(): String = "$obf $deobf"
    }

    private data class Method(val obf: String, val obfSig: String, val deobf: String) {
        override fun toString(): String = "$obf $obfSig $deobf"
    }

    fun toSrg(tsrgFile: File, srgFile: File) {
        // checks
        if (!(srgFile.exists())) srgFile.createNewFile()
        if (srgFile.exists() && !srgFile.isFile) throw RuntimeException("srg path is not a file: $srgFile")
        if (!tsrgFile.exists() || !tsrgFile.isFile) throw RuntimeException("tsrg file not found: $tsrgFile")

        val lines = tsrgFile.readLines()
        val classes = mutableListOf<Clazz>()
        var currentClass: Clazz? = null

        // parse the lines
        lines.forEachIndexed { index, line ->
            if (line.startsWith("\t")) {
                if (currentClass == null) throw RuntimeException("Parse error on line $index: no class")
                val l = line.substring(1, line.length) // get rid of tab
                val parts = l.split(" ")
                when (parts.size) {
                    2 -> {
                        // field
                        val obf = parts[0]
                        val deobf = parts[1]
                        currentClass!!.fields.add(Field(obf, deobf))
                    }
                    3 -> {
                        // method
                        val obf = parts[0]
                        val obfSig = parts[1]
                        val deobf = parts[2]
                        currentClass!!.methods.add(Method(obf, obfSig, deobf))
                    }
                    else -> throw RuntimeException("Parse error on line $index: too many parts")
                }
            } else if (line.contains(" ")) {
                currentClass?.let { classes.add(it) }
                val parts = line.split(" ")
                when (parts.size) {
                    2 -> {
                        // class
                        val obf = parts[0]
                        val deobf = parts[1]
                        currentClass = Clazz(obf, deobf)
                    }
                    else -> throw RuntimeException("Parse error on line $index: class definition has too many parts")
                }
            }
        }

        val classNames = classes.map { it.obf to it.deobf }.toMap()
        val output = StringBuilder()
        // write the classes out in SRG format
        classes.forEach { clazz ->
            if (clazz.obf != clazz.deobf) {
                output.append("CL: ${clazz.obf} ${clazz.deobf}\n")
            }

            clazz.fields.forEach { field ->
                output.append("FD: ${clazz.obf}/${field.obf} ${clazz.deobf}/${field.deobf}\n")
            }

            clazz.methods.forEach { method ->
                // find what classes need to be replaced in the obfuscated string
                val classesToReplace = mutableListOf<String>()
                var buffer = ""
                var state = false
                method.obfSig.forEach {
                    when (it) {
                        'L' -> {
                            buffer = ""
                            state = true
                        }
                        ';' -> {
                            classesToReplace.add(buffer)
                            state = false
                        }
                        else -> {
                            if (state) buffer += it
                        }
                    }
                }

                // replace the obfuscated classes
                var deobfSig = method.obfSig
                classesToReplace.forEach { obfClassName ->
                    if (classNames.containsKey(obfClassName)) {
                        deobfSig = deobfSig.replace("L$obfClassName;", "L${classNames[obfClassName]!!};")
                    }
                }

                output.append("MD: ${clazz.obf}/${method.obf} ${method.obfSig} " +
                        "${clazz.deobf}/${method.deobf} $deobfSig\n")
            }
        }
        srgFile.writeText(output.toString().split("\n").sorted().filter { it.isNotEmpty() }.joinToString("\n"))
    }

    fun fromSrg(srgFile: File, tsrgFile: File) {
        // checks
        if (!(tsrgFile.exists())) tsrgFile.createNewFile()
        if (tsrgFile.exists() && !tsrgFile.isFile) throw RuntimeException("tsrg path is not a file: $tsrgFile")
        if (!srgFile.exists() || !srgFile.isFile) throw RuntimeException("srg file not found: $srgFile")

        val lines = srgFile.readLines()
        val classes = mutableListOf<Clazz>()

        lines.forEach { line ->
            when (true) {
                line.startsWith("CL: ") -> {
                    val l = line.substring(4, line.length)
                    val parts = l.split(" ")
                    val obf = parts[0]
                    val deobf = parts[1]
                    if (!classes.map { it.obf }.contains(obf)) {
                        classes.add(Clazz(obf, deobf))
                    }
                }
                line.startsWith("FD: ") -> {
                    val l = line.substring(4, line.length)
                    val parts = l.split(" ")

                    // obf part
                    val p0 = parts[0]
                    val p0s = p0.lastIndexOf('/')
                    val obfClass = p0.substring(0, p0s)
                    val obf = p0.substring(p0s + 1, p0.length)

                    // deobf part
                    val p1 = parts[1]
                    val p1s = p1.lastIndexOf('/')
                    val deobfClass = p1.substring(0, p1s)
                    val deobf = p1.substring(p1s + 1, p1.length)

                    val eligibleClasses = classes.filter { it.obf == obfClass && it.deobf == deobfClass }
                    if (eligibleClasses.isNotEmpty()) {
                        eligibleClasses.last().fields.add(Field(obf, deobf))
                    } else {
                        // this *shouldn't* happen but just in case the ordering of the mappings is weird we will
                        // add the class to the map
                        val newClass = Clazz(obfClass, deobfClass, mutableListOf(Field(obf, deobf)))
                        classes.add(newClass)
                    }
                }
                line.startsWith("MD: ") -> {
                    val l = line.substring(4, line.length)
                    val parts = l.split(" ")

                    // obf part
                    val p0 = parts[0]
                    val p0s = p0.lastIndexOf('/')
                    val obfClass = p0.substring(0, p0s)
                    val obf = p0.substring(p0s + 1, p0.length)

                    val obfSig = parts[1]

                    // deobf part
                    val p2 = parts[2]
                    val p2s = p2.lastIndexOf('/')
                    val deobfClass = p2.substring(0, p2s)
                    val deobf = p2.substring(p2s + 1, p2.length)

                    val eligibleClasses = classes.filter { it.obf == obfClass && it.deobf == deobfClass }
                    if (eligibleClasses.isNotEmpty()) {
                        eligibleClasses.last().methods.add(Method(obf, obfSig, deobf))
                    } else {
                        val newClass = Clazz(obfClass, deobfClass, mutableListOf(), mutableListOf(Method(obf, obfSig, deobf)))
                        classes.add(newClass)
                    }
                }
            }
        }

        val output = StringBuilder()
        classes.forEach { clazz ->
            output.append("$clazz\n")
            clazz.fields.forEach {
                output.append("\t$it\n")
            }
            clazz.methods.forEach {
                output.append("\t$it\n")
            }
        }
        tsrgFile.writeText(output.toString())
    }

}
