package st.wing.query2fun

import java.io.File
import java.net.JarURLConnection
import java.util.jar.JarEntry
import java.util.jar.JarFile

class ClassPath {
    companion object {
        private val classMap: Map<String, Class<Any>> = mutableMapOf()
        inline fun <reified T> getClassWithAnnotation(pkgName: String): List<Class<*>> {
            return ClassPath.getClass(pkgName).filter {
                it.annotations
                    .any { annotation -> annotation.annotationClass.qualifiedName == T::class.java.name }
            }
        }

        fun getClass(pkgName: String, filter: (String) -> Boolean = { _ -> true }): List<Class<*>> {
            return Thread.currentThread().contextClassLoader
                .getResource(pkgName.replace(".", "/"))
                .let {
                    return when (it.protocol) {
                        "file" -> getClassByFile(File(it.file), pkgName)
                        "jar" -> getClassByJar(it.openConnection() as JarURLConnection)
                        else -> listOf()
                    }.filter(filter).mapNotNull { name ->
                        try {
                            Class.forName(name)
                        } catch (t: Throwable) {
                            null
                        }
                    }
                }
        }

        private fun getClassByJar(connect: JarURLConnection): List<String> {
            return connect.jarFile.entries().toList().filter {
                it.name.toLowerCase().endsWith(".class")
            }.map {
                it.name.substring(0, it.name.length - 6).replace('/', '.')
            }
        }


        private fun getClassByFile(file: File, prefix: String = ""): List<String> {
            val classes: MutableList<String> = mutableListOf();
            val getPrefix = { p: String -> if (prefix.isEmpty()) p else "${prefix}.${p}" }
            if (file.isDirectory) {
                file.listFiles()
                    .filter { it.isDirectory || it.extension == "class" }
                    .groupBy { it.isDirectory }
                    .forEach { (isDir, fs) ->
                        when (isDir) {
                            true -> fs.forEach { dir ->
                                classes.addAll(getClassByFile(dir, getPrefix(file.name)))
                            }
                            false -> fs.forEach { child ->
                                classes.add(getPrefix(child.nameWithoutExtension))
                            }
                        }
                    }
            }
            return classes
        }


    }
}
