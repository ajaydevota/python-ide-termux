package com.sarvam.pythonide

import android.content.Context
import android.os.Build
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Real Termux environment inside the app.
 *
 * applicationId is "com.termux", so context.filesDir is
 * /data/data/com.termux/files — exactly the prefix Termux binaries were built for.
 *
 * The bootstrap is bundled inside the APK (assets/termux/), so no download is needed.
 */
object TermuxEnv {

    private const val BOOTSTRAP_TAG = "bootstrap-2026.10.04-r1%2Bapt.android-7"
    private const val MIN_VALID_SIZE = 30_000_000L

    fun prefix(context: Context) = File(context.filesDir, "usr")
    fun homeDir(context: Context) = File(context.filesDir, "home")
    private fun staging(context: Context) = File(context.filesDir, "usr-staging")

    fun arch(): String {
        val a = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return if (a.contains("x86_64")) "x86_64" else "aarch64"
    }

    private fun bootstrapUrl(): String =
        "https://github.com/termux/termux-packages/releases/download/" +
                BOOTSTRAP_TAG + "/bootstrap-" + arch() + ".zip"

    fun isInstalled(context: Context) = File(prefix(context), "bin/bash").exists()

    private fun chmodX(f: File) {
        try {
            Os.chmod(f.absolutePath, 448) // 0700
        } catch (ignored: Exception) {
        }
    }

    /** Opens the bundled bootstrap asset for this device's architecture. */
    private fun openBundled(context: Context): InputStream? {
        val candidates = listOf(
            "termux/bootstrap-" + arch() + ".bin",
            "termux/bootstrap-" + arch() + ".zip",
            "termux/bootstrap-aarch64.bin"
        )
        for (n in candidates) {
            try {
                return context.assets.open(n)
            } catch (ignored: Exception) {
            }
        }
        return null
    }

    @Synchronized
    fun ensureBootstrap(context: Context, onLog: (String) -> Unit): String {
        if (isInstalled(context)) return "Termux environment pehle se ready hai"

        val prefix = prefix(context)
        val staging = staging(context)

        try {
            onLog("Extract ho raha hai…")
            staging.deleteRecursively()
            staging.mkdirs()

            val symlinks = ArrayList<Pair<String, String>>()
            val bundled = openBundled(context)
            val input: InputStream = bundled ?: downloadToCache(context, onLog)

            input.use { raw ->
                ZipInputStream(raw.buffered()).use { zin ->
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
            if (!staging.renameTo(prefix)) throw IOException("staging move nahi hua")
            homeDir(context).mkdirs()
            File(prefix, "tmp").mkdirs()
            onLog("Termux environment ready")
            return "OK: Termux environment install ho gaya"
        } catch (e: Exception) {
            staging.deleteRecursively()
            return "ERROR: " + e.message
        }
    }

    /** Fallback: download the bootstrap if it was not bundled. */
    private fun downloadToCache(context: Context, onLog: (String) -> Unit): InputStream {
        val dest = File(context.filesDir, "bootstrap.zip")
        var last: Exception? = null
        for (attempt in 1..3) {
            try {
                if (attempt > 1) onLog("Retry " + attempt + "/3…")
                val conn = URL(bootstrapUrl()).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 60000
                conn.readTimeout = 180000
                conn.setRequestProperty("Accept-Encoding", "identity")
                conn.setRequestProperty("User-Agent", "PythonIDE")
                val expected = conn.contentLengthLong
                conn.inputStream.use { inp ->
                    FileOutputStream(dest).use { out -> inp.copyTo(out) }
                }
                val got = dest.length()
                if (expected > 0 && got != expected) throw IOException("size mismatch $got/$expected")
                if (got < MIN_VALID_SIZE) throw IOException("file chhoti hai ($got)")
                return dest.inputStream()
            } catch (e: Exception) {
                last = e
                dest.delete()
            }
        }
        throw last ?: IOException("download failed")
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
