package com.sarvam.pythonide

import android.content.Context
import android.os.Build
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Real Termux environment inside the app.
 *
 * applicationId is "com.termux", so context.filesDir is
 * /data/data/com.termux/files — exactly the prefix Termux binaries were built for.
 */
object TermuxEnv {

    private const val BOOTSTRAP_TAG = "bootstrap-2026.10.04-r1%2Bapt.android-7"

    // The bootstrap zip is ~33 MB; anything much smaller is a truncated/bad download.
    private const val MIN_VALID_SIZE = 30_000_000L

    fun prefix(context: Context) = File(context.filesDir, "usr")
    fun homeDir(context: Context) = File(context.filesDir, "home")
    private fun staging(context: Context) = File(context.filesDir, "usr-staging")
    private fun zipFile(context: Context) = File(context.filesDir, "bootstrap.zip")

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
        val zip = zipFile(context)

        try {
            if (!zip.exists() || zip.length() < MIN_VALID_SIZE) {
                onLog("Bootstrap download ho raha hai (~33 MB)…")
                zip.delete()
                download(bootstrapUrl(), zip, onLog)
                onLog("Download poora hua: " + zip.length() / 1_000_000 + " MB")
            } else {
                onLog("Pehle se downloaded file mili: " + zip.length() / 1_000_000 + " MB")
            }

            onLog("Extract ho raha hai…")
            staging.deleteRecursively()
            staging.mkdirs()

            val symlinks = ArrayList<Pair<String, String>>()
            ZipInputStream(zip.inputStream().buffered()).use { zin ->
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
            if (!staging.renameTo(prefix)) throw IOException("staging move nahi hua")
            homeDir(context).mkdirs()
            File(prefix, "tmp").mkdirs()
            zip.delete()
            onLog("Termux environment ready")
            return "OK: Termux environment install ho gaya"
        } catch (e: Exception) {
            // Delete the bad zip so the next attempt downloads again.
            zip.delete()
            staging.deleteRecursively()
            return "ERROR: " + e.message + "  (dobara try karein — screen band karke phir kholo)"
        }
    }

    /** Downloads with size verification and up to 3 attempts. */
    private fun download(url: String, dest: File, onLog: (String) -> Unit) {
        var last: Exception? = null
        for (attempt in 1..3) {
            try {
                if (attempt > 1) onLog("Retry " + attempt + "/3…")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 60000
                conn.readTimeout = 180000
                conn.setRequestProperty("Accept-Encoding", "identity")
                conn.setRequestProperty("User-Agent", "PythonIDE")
                val expected = conn.contentLengthLong
                conn.inputStream.use { input ->
                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                }
                val got = dest.length()
                if (expected > 0 && got != expected) {
                    throw IOException("size mismatch: got " + got + " expected " + expected)
                }
                if (got < MIN_VALID_SIZE) {
                    throw IOException("file bahut chhoti hai (" + got + " bytes)")
                }
                return
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
