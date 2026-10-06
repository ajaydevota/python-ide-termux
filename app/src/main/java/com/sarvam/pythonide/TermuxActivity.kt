package com.sarvam.pythonide

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Termux-style terminal. Output is streamed live as the command runs.
 */
class TermuxActivity : AppCompatActivity() {

    private lateinit var term: EditText
    private var busy = false

    // Termux's own welcome message (motd)
    private val welcome = """
Welcome to Termux!

Docs:       https://termux.dev/docs
Donate:     https://termux.dev/donate
Community:  https://termux.dev/community

Working with packages:

 - Search:  pkg search <query>
 - Install: pkg install <package>
 - Upgrade: pkg upgrade

Subscribing to additional repositories:

 - Root:    pkg install root-repo
 - X11:     pkg install x11-repo

For fixing any repository issues,
try 'termux-change-repo' command.

Report issues at https://termux.dev/issues

""".trimIndent() + "\n"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux)

        term = findViewById(R.id.termInput)

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.enterBtn).setOnClickListener { submit() }

        term.setOnEditorActionListener { _, actionId, event ->
            val enterKey = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO || enterKey
            ) {
                submit()
                true
            } else {
                false
            }
        }
        term.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                submit()
                true
            } else {
                false
            }
        }

        write(welcome)

        lifecycleScope.launch {
            val s = withContext(Dispatchers.Default) {
                TermuxEnv.ensureBootstrap(this@TermuxActivity) { line ->
                    runOnUiThread { write(line + "\n") }
                }
            }
            write(s + "\n\n$ ")
        }
    }

    private fun submit() {
        if (busy) return
        val full = term.text.toString()
        val nl = full.lastIndexOf('\n')
        var line = if (nl >= 0) full.substring(nl + 1) else full
        if (line.startsWith("$ ")) line = line.substring(2)
        val cmd = line.trim()

        busy = true
        write("\n")
        lifecycleScope.launch {
            if (cmd.isNotEmpty()) {
                // Stream output live
                val code = withContext(Dispatchers.Default) {
                    TermuxEnv.runStreaming(this@TermuxActivity, cmd) { chunk ->
                        runOnUiThread { write(chunk) }
                    }
                }
                if (code != 0) write("[exit " + code + "]\n")
            }
            write("\n$ ")
            busy = false
        }
    }

    private fun write(s: String) {
        term.append(s)
        term.setSelection(term.text.length)
    }
}
