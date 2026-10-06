package com.sarvam.pythonide

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }

        val fontSeek = findViewById<SeekBar>(R.id.fontSeek)
        val tabSeek = findViewById<SeekBar>(R.id.tabSeek)
        val keysSwitch = findViewById<SwitchCompat>(R.id.keysSwitch)
        val fontValue = findViewById<TextView>(R.id.fontValue)
        val tabValue = findViewById<TextView>(R.id.tabValue)

        fontSeek.max = 20
        fontSeek.progress = Prefs.fontSize(this) - 10
        tabSeek.max = 6
        tabSeek.progress = Prefs.tabSize(this) - 2
        keysSwitch.isChecked = Prefs.showKeys(this)
        fontValue.text = Prefs.fontSize(this).toString()
        tabValue.text = Prefs.tabSize(this).toString()

        fontSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = p + 10
                fontValue.text = v.toString()
                Prefs.setFontSize(this@SettingsActivity, v)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        tabSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = p + 2
                tabValue.text = v.toString()
                Prefs.setTabSize(this@SettingsActivity, v)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        keysSwitch.setOnCheckedChangeListener { _, v ->
            Prefs.setShowKeys(this@SettingsActivity, v)
        }
    }
}
