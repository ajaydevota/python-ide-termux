package com.sarvam.pythonide

import android.content.Context
import android.os.Build
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Real Termux environment inside the app.
 *
 * The app's applicationId is "com.termux", so context.filesDir is
 * /data/data/com.termux/files — which is exactly the prefix Termux binaries
 * were compiled for. That is why no package rebuild is needed.
 */
object TermuxEnv {

    // Termux bootstrap release (apt-android-7 variant), published by termux-packages.
    private const val BOOTSTRAP_TAG = "bootstrap-2026.10.04-r1%2Bapt.android-7"

    fun prefix(context: Context) = File(context.filesDir, "usr")
    fun homeDir(context: Context) = File(context.filesDir, "home")
    private fun staging(context: Context) = File(context.filesDir, "usr-staging")

    fun arch(): String {
        val a = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return if (a.contains("x86_64")) "x86_64" else "aarch64"
    }

    fun bootstrapUrl(): String =
        "https://github.com/termux/termux-packages/releases/download/" +
                BOOTSTRAP_TAG + "/bootstrap-" + arch() + ".zip"

    fun isInstalled(context: Context) = File(prefix(context), "bin/bash").exists()

    private fun chmodX(f: File) {
        try {
            Os.chmod(f.absolutePath, 448) // 0700
        } catch (ignored: Exception) {
        }
    }

    @Synchronized
    fun ensureBootstrap(context: Context, onLog: (String) -> Unit): String {
        if (isInstalled(context)) return "Termux environment pehle se ready hai"

        val prefix = prefix(context)
        val staging = staging(context)
        val zipFile = File(context.filesDir, "bootstrap.zip")

        try {
            if (!zipFile.exists() || zipFile.length() < 1_000_000) {
                onLog("Bootstrap download ho raha hai (~33 MB)…")
                download(bootstrapUrl(), zipFile)
            }
            onLog("Extract ho raha hai…")
            staging.deleteRecursively()
            staging.mkdirs()

            val symlinks = ArrayList<Pair<String, String>>()
            ZipInputStream(zipFile.inputStream().buffered()).use { zin ->
                var e = zin.nextEntry
                while (e != null) {
                    val name = e.name
                    if (name == "SYMLINKS.txt") {
                        val txt = zin.bufferedReader().readText()
                        txt.split("\n").forEach { line ->
                            val parts = line.split("\u2190")
                            if (parts.size == 2) symlinks.add(Pair(parts[0], parts[1]))
                        }
                    } else {
                        val out = File(staging, name)
                        if (e.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            FileOutputStream(out).use { fos -> zin.copyTo(fos) }
                            if (name.startsWith("bin/") || name.startsWith("libexec") ||
                                name.startsWith("lib/apt/apt-helper") ||
                                name.startsWith("lib/apt/methods")
                            ) {
                                chmodX(out)
                            }
                        }
                    }
                    e = zin.nextEntry
                }
            }

            onLog("Symlinks: " + symlinks.size)
            for ((target, link) in symlinks) {
                val linkPath = File(staging, link)
                linkPath.parentFile?.mkdirs()
                try {
                    if (linkPath.exists()) linkPath.delete()
                    Os.symlink(target, linkPath.absolutePath)
                } catch (ignored: Exception) {
                }
            }

            prefix.deleteRecursively()
            if (!staging.renameTo(prefix)) return "ERROR: staging move nahi hua"
            homeDir(context).mkdirs()
            File(prefix, "tmp").mkdirs()
            onLog("Termux environment ready")
            return "OK: Termux environment install ho gaya"
        } catch (e: Exception) {
            return "ERROR: " + e.message
        }
    }

    private fun download(url: String, dest: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30000
        conn.readTimeout = 180000
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { out -> input.copyTo(out) }
        }
    }

    fun env(context: Context): Map<String, String> {
        val prefix = prefix(context).absolutePath
        val m = HashMap<String, String>()
        m["HOME"] = homeDir(context).absolutePath
        m["PREFIX"] = prefix
        m["PATH"] = prefix + "/bin:" + prefix + "/bin/applets"
        m["LD_LIBRARY_PATH"] = prefix + "/lib"
        m["TMPDIR"] = prefix + "/tmp"
        m["TERM"] = "xterm-256color"
        m["LANG"] = "en_US.UTF-8"
        m["ANDROID_ROOT"] = "/system"
        m["ANDROID_DATA"] = "/data"
        val exec = File(prefix, "lib/libtermux-exec.so")
        if (exec.exists()) m["LD_PRELOAD"] = exec.absolutePath
        return m
    }

    fun run(context: Context, cmd: String, timeoutSec: Long = 240): String {
        val bash = File(prefix(context), "bin/bash")
        if (!bash.exists()) return "ERROR: Termux bootstrap install nahi hua"
        val pb = ProcessBuilder(bash.absolutePath, "-l", "-c", cmd)
        pb.environment().putAll(env(context))
        pb.redirectErrorStream(true)
        return try {
            val p = pb.start()
            val out = p.inputStream.bufferedReader().readText()
            val done = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!done) {
                p.destroyForcibly()
                out + "\n[timeout " + timeoutSec + "s]"
            } else {
                out
            }
        } catch (e: Exception) {
            "ERROR: " + e.message
        }
    }
}
