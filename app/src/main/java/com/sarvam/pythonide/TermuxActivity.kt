package com.sarvam.pythonide

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TermuxActivity : AppCompatActivity() {

    private val buf = StringBuilder()
    private lateinit var termView: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux)

        termView = findViewById(R.id.termView)
        scroll = findViewById(R.id.termScroll)
        input = findViewById(R.id.cmdInput)

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.enterBtn).setOnClickListener { submit() }

        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN)
            ) {
                submit()
                true
            } else {
                false
            }
        }

        write("Welcome to Termux (modified)\n")
        write("GUI editor bhi hai — Home se project khol kar code likh sakte ho.\n\n")

        lifecycleScope.launch {
            val s = withContext(Dispatchers.Default) {
                TermuxEnv.ensureBootstrap(this@TermuxActivity) { line ->
                    runOnUiThread { write(line + "\n") }
                }
            }
            write(s + "\n\n")
        }
    }

    private fun submit() {
        if (busy) return
        val cmd = input.text.toString().trim()
        if (cmd.isEmpty()) return
        input.setText("")
        write("$ " + cmd + "\n")
        busy = true
        lifecycleScope.launch {
            val out = withContext(Dispatchers.Default) {
                TermuxEnv.run(this@TermuxActivity, cmd)
            }
            write(if (out.endsWith("\n")) out else out + "\n")
            busy = false
        }
    }

    private fun write(s: String) {
        buf.append(s)
        termView.text = buf.toString()
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
