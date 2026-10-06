package com.sarvam.pythonide

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TermuxActivity : AppCompatActivity() {

    private val logBuffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux)

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }
        val input = findViewById<EditText>(R.id.cmdInput)

        log("Termux environment (modified).")
        log("Pehli baar bootstrap download + extract hoga (~33 MB).")
        log("")

        fun chip(id: Int, cmd: String) {
            findViewById<Button>(id).setOnClickListener { input.setText(cmd) }
        }
        chip(R.id.chipUpdate, "pkg update -y")
        chip(R.id.chipPython, "pkg install -y python")
        chip(R.id.chipNumpy, "pip install numpy")
        chip(R.id.chipVer, "python --version")

        findViewById<Button>(R.id.runCmdBtn).setOnClickListener {
            val c = input.text.toString().trim()
            if (c.isNotEmpty()) {
                runCmd(c)
                input.setText("")
            }
        }

        lifecycleScope.launch {
            val s = withContext(Dispatchers.Default) {
                TermuxEnv.ensureBootstrap(this@TermuxActivity) { line -> log("[setup] " + line) }
            }
            log("[setup] " + s)
            log("")
        }
    }

    private fun runCmd(cmd: String) {
        log("$ " + cmd)
        log("chal raha hai…")
        lifecycleScope.launch {
            val out = withContext(Dispatchers.Default) {
                TermuxEnv.run(this@TermuxActivity, cmd)
            }
            log(out)
            log("")
        }
    }

    private fun log(line: String) {
        logBuffer.append(line).append("\n")
        findViewById<TextView>(R.id.logView).text = logBuffer.toString()
    }
}
