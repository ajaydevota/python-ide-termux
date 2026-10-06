package com.sarvam.pythonide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorActivity : AppCompatActivity() {

    private lateinit var editor: CodeEditor
    private lateinit var fileNameView: TextView
    private lateinit var runButton: Button
    private lateinit var keyBar: HorizontalScrollView
    private var currentFile = "main.py"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        currentFile = intent.getStringExtra("file") ?: "main.py"

        val container = findViewById<FrameLayout>(R.id.editorContainer)
        editor = CodeEditor(this)
        container.addView(
            editor,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        EditorSetup.setup(this, editor)
        editor.setTextSize(Prefs.fontSize(this).toFloat())

        fileNameView = findViewById(R.id.fileName)
        runButton = findViewById(R.id.runButton)
        keyBar = findViewById(R.id.keyBar)

        findViewById<TextView>(R.id.backBtn).setOnClickListener {
            FileStore.write(this, currentFile, editor.text.toString())
            finish()
        }

        buildSpecialKeys(findViewById(R.id.keyRow))
        keyBar.visibility = if (Prefs.showKeys(this)) View.VISIBLE else View.GONE

        editor.setText(FileStore.read(this, currentFile) ?: "")
        fileNameView.text = currentFile
        runButton.setOnClickListener { runCode() }
    }

    private fun buildSpecialKeys(row: LinearLayout) {
        val keys = listOf(
            "(", ")", "[", "]", "{", "}", ":", "=",
            "+", "-", "*", "/", "%", "\"", "'", "#",
            "_", ".", ",", "->", "==", "!=", "<=", ">=", "Tab"
        )
        for (k in keys) {
            val b = TextView(this)
            b.text = k
            b.setPadding(28, 22, 28, 22)
            b.setTextColor(0xFFFFFFFF.toInt())
            b.textSize = 14f
            b.background = getDrawable(R.drawable.key_bg)
            b.setOnClickListener { insertKey(k) }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 12
            row.addView(b, lp)
        }
    }

    private fun insertKey(k: String) {
        val text = if (k == "Tab") " ".repeat(Prefs.tabSize(this)) else k
        val cursor = editor.cursor
        editor.text.insert(cursor.leftLine, cursor.leftColumn, text)
    }

    private fun runCode() {
        val code = editor.text.toString()
        FileStore.write(this, currentFile, code)
        runButton.isEnabled = false
        runButton.text = "…"
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                PythonRunner.run(this@EditorActivity, code)
            }
            runButton.isEnabled = true
            runButton.text = "\u25B6 Run"
            val i = Intent(this@EditorActivity, OutputActivity::class.java)
            i.putExtra("output", result.text)
            i.putExtra("image", result.imagePath)
            startActivity(i)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            FileStore.write(this, currentFile, editor.text.toString())
        } catch (ignored: Exception) {
        }
    }
}
