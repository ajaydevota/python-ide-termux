package com.sarvam.pythonide

import android.content.Context
import com.chaquo.python.Python
import org.json.JSONObject
import java.io.File

object PythonRunner {

    data class RunResult(val text: String, val imagePath: String?)

    private fun outDir(context: Context): File {
        val d = File(context.filesDir, "outputs")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun run(context: Context, code: String): RunResult {
        return try {
            val module = Python.getInstance().getModule("runner")
            val json = module.callAttr("run", code, outDir(context).absolutePath).toString()

            val obj = JSONObject(json)
            val text = obj.optString("text", "")
            var imagePath: String? = null
            val arr = obj.optJSONArray("artifacts")
            if (arr != null && arr.length() > 0) {
                imagePath = arr.getJSONObject(0).optString("path")
            }
            RunResult(text, imagePath)
        } catch (e: Exception) {
            RunResult("Error: " + e.message, null)
        }
    }

    fun checkModule(context: Context, name: String): Boolean {
        return try {
            val module = Python.getInstance().getModule("runner")
            module.callAttr("check_module", name).toBoolean()
        } catch (e: Exception) {
            false
        }
    }

    fun pipInstall(context: Context, name: String): String {
        return try {
            val module = Python.getInstance().getModule("runner")
            module.callAttr("pip_install", name, outDir(context).absolutePath).toString()
        } catch (e: Exception) {
            "ERROR: " + e.message
        }
    }

    fun gitClone(context: Context, url: String): String {
        return try {
            val module = Python.getInstance().getModule("runner")
            module.callAttr("git_clone", url, outDir(context).absolutePath).toString()
        } catch (e: Exception) {
            "ERROR: " + e.message
        }
    }
}
