package com.sarvam.pythonide

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OutputActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_output)

        val text = intent.getStringExtra("output") ?: ""
        val image = intent.getStringExtra("image")

        val consoleScroll = findViewById<ScrollView>(R.id.consoleScroll)
        val consoleView = findViewById<TextView>(R.id.consoleView)
        val graphicsView = findViewById<ImageView>(R.id.graphicsView)

        consoleView.text = text.ifBlank { "(कोई आउटपुट नहीं)" }

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }

        var hasImage = false
        if (image != null) {
            val bmp = BitmapFactory.decodeFile(image)
            if (bmp != null) {
                graphicsView.setImageBitmap(bmp)
                hasImage = true
            }
        }

        fun showConsole() {
            consoleScroll.visibility = View.VISIBLE
            graphicsView.visibility = View.GONE
        }

        fun showGraphics() {
            consoleScroll.visibility = View.GONE
            graphicsView.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.tabConsole).setOnClickListener { showConsole() }
        findViewById<Button>(R.id.tabGraphics).setOnClickListener { showGraphics() }

        // Auto-detect: graphics if the code produced an image, else console.
        if (hasImage) showGraphics() else showConsole()
    }
}
