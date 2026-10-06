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
 * Termux-style terminal: the transcript and the input are the SAME text area
 * (like a real terminal), not a separate box.
 */
class TermuxActivity : AppCompatActivity() {

    private lateinit var term: EditText
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux)

        term = findViewById(R.id.termInput)

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.enterBtn).setOnClickListener { submit() }

        // IME "send" key (soft keyboard)
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

        // Hardware Enter
        term.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                submit()
                true
            } else {
                false
            }
        }

        write("Welcome to Termux (modified)\n")
        write("GUI editor bhi hai — Home se project khol kar code likh sakte ho.\n\n")
        write("$ ")

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
        lifecycleScope.launch {
            write("\n")
            if (cmd.isNotEmpty()) {
                val out = withContext(Dispatchers.Default) {
                    TermuxEnv.run(this@TermuxActivity, cmd)
                }
                write(out)
                if (!out.endsWith("\n")) write("\n")
            }
            write("$ ")
            busy = false
        }
    }

    private fun write(s: String) {
        term.append(s)
        term.setSelection(term.text.length)
    }
}
