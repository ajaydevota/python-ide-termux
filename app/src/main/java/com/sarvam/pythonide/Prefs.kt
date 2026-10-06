package com.sarvam.pythonide

import android.content.Context

object Prefs {
    private const val NAME = "pythonide_prefs"

    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun fontSize(c: Context) = sp(c).getInt("font", 14)
    fun setFontSize(c: Context, v: Int) { sp(c).edit().putInt("font", v).apply() }

    fun tabSize(c: Context) = sp(c).getInt("tab", 4)
    fun setTabSize(c: Context, v: Int) { sp(c).edit().putInt("tab", v).apply() }

    fun showKeys(c: Context) = sp(c).getBoolean("keys", true)
    fun setShowKeys(c: Context, v: Boolean) { sp(c).edit().putBoolean("keys", v).apply() }
}
