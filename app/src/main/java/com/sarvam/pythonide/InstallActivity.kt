package com.sarvam.pythonide

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstallActivity : AppCompatActivity() {

    private val logBuffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_install)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val input = findViewById<EditText>(R.id.cmdInput)
        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }

        log("पैकेज इंस्टॉल (runtime) और GitHub clone - dono internet se.")
        log("• pip install: pure-Python packages (requests, flask, sympy, six...)")
        log("• git clone: GitHub repo ka zip download karke extract karega")
        log("")

        fun chip(id: Int, value: String) {
            findViewById<Button>(id).setOnClickListener { input.setText(value) }
        }
        chip(R.id.chipRequests, "requests")
        chip(R.id.chipFlask, "flask")
        chip(R.id.chipSympy, "sympy")
        chip(R.id.chipClone, "https://github.com/psf/requests")

        findViewById<Button>(R.id.pipBtn).setOnClickListener {
            val raw = input.text.toString().trim()
            val name = raw.removePrefix("pip install").trim().split(" ").firstOrNull()?.trim() ?: ""
            if (name.isEmpty()) {
                log("नाम खाली है।")
                return@setOnClickListener
            }
            log("$ pip install " + name)
            log("डाउनलोड हो रहा है…")
            lifecycleScope.launch {
                val res = withContext(Dispatchers.Default) {
                    PythonRunner.pipInstall(this@InstallActivity, name)
                }
                log(res)
                log("")
                input.setText("")
            }
        }

        findViewById<Button>(R.id.cloneBtn).setOnClickListener {
            val url = input.text.toString().trim()
            if (url.isEmpty()) {
                log("GitHub URL खाली है।")
                return@setOnClickListener
            }
            log("$ git clone " + url)
            log("डाउनलोड हो रहा है…")
            lifecycleScope.launch {
                val res = withContext(Dispatchers.Default) {
                    PythonRunner.gitClone(this@InstallActivity, url)
                }
                log(res)
                log("")
                input.setText("")
            }
        }
    }

    private fun log(line: String) {
        logBuffer.append(line).append("\n")
        findViewById<TextView>(R.id.logView).text = logBuffer.toString()
    }
}
