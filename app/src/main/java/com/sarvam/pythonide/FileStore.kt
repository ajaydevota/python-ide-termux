package com.sarvam.pythonide

import android.content.Context
import java.io.File

object FileStore {

    private fun dir(context: Context): File {
        val d = File(context.filesDir, "scripts")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun read(context: Context, name: String): String? {
        val f = File(dir(context), name)
        return if (f.exists()) f.readText() else null
    }

    fun write(context: Context, name: String, content: String) {
        File(dir(context), name).writeText(content)
    }

    fun list(context: Context): List<String> {
        return dir(context).listFiles { f -> f.name.endsWith(".py") }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun rename(context: Context, oldName: String, newName: String): Boolean {
        val old = File(dir(context), oldName)
        val new = File(dir(context), newName)
        return old.exists() && old.renameTo(new)
    }

    fun delete(context: Context, name: String): Boolean {
        return File(dir(context), name).delete()
    }
}
