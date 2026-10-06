package com.sarvam.pythonide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyHint: TextView
    private var projects = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        listView = findViewById(R.id.projectList)
        emptyHint = findViewById(R.id.emptyHint)

        findViewById<TextView>(R.id.menuDots).setOnClickListener { showMenu(it) }
        findViewById<Button>(R.id.addFab).setOnClickListener { newProject() }

        listView.setOnItemClickListener { _, _, pos, _ -> openProject(projects[pos]) }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            projectMenu(projects[pos])
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        projects = FileStore.list(this).toMutableList()
        listView.adapter = ArrayAdapter(this, R.layout.item_project, R.id.itemName, projects)
        emptyHint.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Menu: only Settings and Termux. */
    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "सेटिंग्स")
        popup.menu.add(0, 2, 1, "Termux")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> startActivity(Intent(this, SettingsActivity::class.java))
                2 -> startActivity(Intent(this, TermuxActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun newProject() {
        val input = EditText(this)
        input.hint = "नाम (जैसे test.py)"
        AlertDialog.Builder(this)
            .setTitle("नया प्रोजेक्ट")
            .setView(input)
            .setPositiveButton("बनाएँ") { _, _ ->
                var n = input.text.toString().trim()
                if (n.isEmpty()) n = "untitled"
                if (!n.endsWith(".py")) n += ".py"
                FileStore.write(this, n, DEFAULT_CODE)
                refresh()
                openProject(n)
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    private fun projectMenu(name: String) {
        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(arrayOf("नाम बदलें", "हटाएँ")) { _, which ->
                when (which) {
                    0 -> renameProject(name)
                    1 -> deleteProject(name)
                }
            }
            .show()
    }

    private fun renameProject(name: String) {
        val input = EditText(this)
        input.setText(name)
        AlertDialog.Builder(this)
            .setTitle("नाम बदलें")
            .setView(input)
            .setPositiveButton("ठीक") { _, _ ->
                var n = input.text.toString().trim()
                if (n.isNotEmpty()) {
                    if (!n.endsWith(".py")) n += ".py"
                    FileStore.rename(this, name, n)
                    refresh()
                }
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    private fun deleteProject(name: String) {
        AlertDialog.Builder(this)
            .setTitle("हटाएँ?")
            .setMessage(name + " हटा दें?")
            .setPositiveButton("हटाएँ") { _, _ ->
                FileStore.delete(this, name)
                refresh()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    private fun openProject(name: String) {
        val i = Intent(this, EditorActivity::class.java)
        i.putExtra("file", name)
        startActivity(i)
    }

    companion object {
        const val DEFAULT_CODE =
            "# नमस्ते! अपना पाइथन कोड यहाँ लिखें\n" +
            "print(\"Hello, Ajay\")\n"
    }
}
