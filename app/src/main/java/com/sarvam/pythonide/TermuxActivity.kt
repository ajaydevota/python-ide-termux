package com.sarvam.pythonide

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real Termux terminal. Launcher screen.
 * `eg <filename>` (installed into $PREFIX/bin) opens the GUI editor.
 */
class TermuxActivity : AppCompatActivity(), TerminalViewClient, TerminalSessionClient {

    private lateinit var terminalView: TerminalView
    private var session: TerminalSession? = null

    private var ctrlDown = false
    private var altDown = false
    private var shiftDown = false
    private var fnDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux)

        terminalView = findViewById(R.id.terminalView)
        terminalView.setTerminalViewClient(this)
        terminalView.setTextSize(20)
        terminalView.requestFocus()

        findViewById<TextView>(R.id.menuDots).setOnClickListener { showMenu(it) }

        buildExtraKeys()

        terminalView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (terminalView.width > 0 && terminalView.height > 0) {
                        terminalView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        lifecycleScope.launch {
                            withContext(Dispatchers.Default) {
                                TermuxEnv.ensureBootstrap(this@TermuxActivity) { }
                            }
                            createSession()
                        }
                    }
                }
            }
        )
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "सेटिंग्स")
        popup.menu.add(0, 2, 1, "Termux (reload)")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> startActivity(Intent(this, SettingsActivity::class.java))
                2 -> recreate()
            }
            true
        }
        popup.show()
    }

    private fun createSession() {
        if (session != null) return
        val prefix = TermuxEnv.prefix(this).absolutePath
        val home = TermuxEnv.homeDir(this).absolutePath
        val login = File(prefix, "bin/login")
        val shellPath = if (login.exists()) login.absolutePath else prefix + "/bin/bash"
        val envArr = TermuxEnv.env(this).map { (k, v) -> k + "=" + v }.toTypedArray()
        val s = TerminalSession(shellPath, home, arrayOf("-l"), envArr, 2000, this)
        session = s
        terminalView.attachSession(s)
    }

    private fun buildExtraKeys() {
        val row = findViewById<LinearLayout>(R.id.extraKeys)
        fun add(label: String, onTap: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.textSize = 11f
            b.minWidth = 0
            b.minimumWidth = 0
            b.setPadding(20, 6, 20, 6)
            b.setOnClickListener { onTap() }
            row.addView(b)
        }
        add("ESC") { sendKey("\u001b") }
        add("CTRL") { ctrlDown = !ctrlDown }
        add("ALT") { altDown = !altDown }
        add("TAB") { sendKey("\t") }
        add("\u2190") { sendKey("\u001b[D") }
        add("\u2191") { sendKey("\u001b[A") }
        add("\u2193") { sendKey("\u001b[B") }
        add("\u2192") { sendKey("\u001b[C") }
        add("-") { sendKey("-") }
        add("/") { sendKey("/") }
        add("|") { sendKey("|") }
        add("~") { sendKey("~") }
    }

    private fun sendKey(s: String) {
        val b = s.toByteArray()
        session?.write(b, 0, b.size)
        terminalView.requestFocus()
    }

    // ---------------- TerminalViewClient ----------------
    override fun onScale(scale: Float): Float = scale
    override fun onSingleTapUp(e: MotionEvent) {
        terminalView.requestFocus()
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = ctrlDown
    override fun readAltKey(): Boolean = altDown
    override fun readShiftKey(): Boolean = shiftDown
    override fun readFnKey(): Boolean = fnDown
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() {}

    // ---------------- TerminalSessionClient ----------------
    override fun onTextChanged(changedSession: TerminalSession) {
        terminalView.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) {}
    override fun onSessionFinished(finishedSession: TerminalSession) {}
    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun getTerminalCursorStyle(): Int = TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Termux", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val t = clip.getItemAt(0).coerceToText(this).toString()
            val b = t.toByteArray()
            session.write(b, 0, b.size)
        }
    }

    // ---------------- log (both interfaces) ----------------
    override fun logError(tag: String, message: String) {}
    override fun logWarn(tag: String, message: String) {}
    override fun logInfo(tag: String, message: String) {}
    override fun logDebug(tag: String, message: String) {}
    override fun logVerbose(tag: String, message: String) {}
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
    override fun logStackTrace(tag: String, e: Exception) {}

    override fun onDestroy() {
        super.onDestroy()
        session?.finishIfRunning()
    }
}
