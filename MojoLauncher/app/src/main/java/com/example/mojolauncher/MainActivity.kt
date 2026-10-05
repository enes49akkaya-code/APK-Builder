package com.example.mojolauncher

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class AppInfo(val label: String, val pkg: String, val icon: Drawable)

class MainActivity : AppCompatActivity() {

    private var allApps = listOf<AppInfo>()
    private val adapter = AppAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<RecyclerView>(R.id.apps).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = this@MainActivity.adapter
        }
        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        // Launcher ana ekranda geri tuşuna basınca kapanmasın
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        allApps = packageManager.queryIntentActivities(intent, 0)
            .map { AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.loadIcon(packageManager)) }
            .filter { it.pkg != packageName }
            .sortedBy { it.label.lowercase() }
        filter(findViewById<EditText>(R.id.search).text.toString())
    }

    private fun filter(q: String) {
        adapter.submit(if (q.isBlank()) allApps else allApps.filter { it.label.contains(q, true) })
    }

    private fun launch(app: AppInfo) {
        packageManager.getLaunchIntentForPackage(app.pkg)?.let { startActivity(it) }
    }

    private fun showMenu(v: View, app: AppInfo) {
        PopupMenu(this, v).apply {
            menu.add("Uygulama bilgisi")
            menu.add("Kaldır")
            setOnMenuItemClickListener {
                val uri = Uri.parse("package:${app.pkg}")
                startActivity(
                    if (it.title == "Kaldır") Intent(Intent.ACTION_DELETE, uri)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                )
                true
            }
            show()
        }
    }

    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        private var items = listOf<AppInfo>()
        fun submit(list: List<AppInfo>) { items = list; notifyDataSetChanged() }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val label: TextView = v.findViewById(R.id.label)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false))

        override fun onBindViewHolder(h: VH, i: Int) {
            val app = items[i]
            h.icon.setImageDrawable(app.icon)
            h.label.text = app.label
            h.itemView.setOnClickListener { launch(app) }
            h.itemView.setOnLongClickListener { showMenu(it, app); true }
        }

        override fun getItemCount() = items.size
    }
}
