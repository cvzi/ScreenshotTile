package com.github.cvzi.screenshottile.activities


import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.ScreenshotHistoryAdapter
import com.github.cvzi.screenshottile.utils.SingleImage
import com.github.cvzi.screenshottile.utils.cleanUpAppData
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.util.*


/**
 * View recent screenshots especially files in /Android/data folder
 */
class HistoryActivity : AppCompatActivity() {
    companion object {
        const val TAG = "HistoryActivity.kt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        findViewById<Button>(R.id.buttonClear).setOnClickListener {
            clear()
        }

        findViewById<SwitchMaterial>(R.id.switchKeepHistory).setOnCheckedChangeListener { v, isChecked ->
            val prefManager = App.getInstance().prefManager
            if (isChecked != prefManager.keepScreenshotHistory) {
                v.setText(if (isChecked) R.string.notification_settings_on else R.string.notification_settings_off)
                prefManager.keepScreenshotHistory = isChecked
                if (!isChecked) {
                    clear()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clear() {
        val folder = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.toString()
            ?: "Android/data/com.github.cvzi.screenshottile/..."
        AlertDialog.Builder(this).apply {
            title = getString(R.string.button_clear)
            setMessage(getString(R.string.button_clear_confirm, folder))
        }.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            cleanUpAppData(this@HistoryActivity, 0) {
                val adapter =
                    findViewById<RecyclerView>(R.id.recyclerView).adapter as? ScreenshotHistoryAdapter
                adapter?.run {
                    dataSet = loadImageList()
                    notifyDataSetChanged()
                }
            }
        }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }.show()
    }

    override fun onResume() {
        super.onResume()

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager

        val data = loadImageList()

        val adapter = ScreenshotHistoryAdapter(this, data) { record, view ->
            Log.v(TAG, "Click on history item: $record $view")
        }
        recyclerView.adapter = adapter

        findViewById<SwitchMaterial>(R.id.switchKeepHistory).isChecked =
            App.getInstance().prefManager.keepScreenshotHistory
    }

    private fun loadImageList(): ArrayList<SingleImage> {
        // Add files from /Android/data folder
        val allFiles = HashMap<File, Boolean>()
        val allUris = HashMap<Uri, Boolean>()
        val folder = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileList =
            folder?.listFiles { _, string ->
                string.endsWith("jpg", true)
                        || string.endsWith("jpeg", true)
                        || string.endsWith("png", true)
                        || string.endsWith("webp", true)
            }?.map { file ->
                SingleImage(
                    FileProvider.getUriForFile(
                        this,
                        EditImageActivity.FILE_PROVIDER_AUTHORITY,
                        file
                    ),
                    file,
                    folder.absolutePath,
                    lastModified = file?.lastModified()?.run { Date(this) },
                    isAppData = true
                )
            }?.toMutableList() ?: listOf()

        var data = ArrayList(fileList)

        // Add files from history
        for (item in App.getInstance().prefManager.screenshotHistory) {
            data.add(SingleImage(item.uri, item.file, lastModified = item.date))
        }

        data = ArrayList(data.filter {
            it.uri !in allUris || it.file == null || it.file !in allFiles
        })
        data.sortByDescending { it.lastModified }
        return data
    }

}


