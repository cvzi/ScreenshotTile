package com.github.cvzi.screenshottile.activities


import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityHistoryBinding
import com.github.cvzi.screenshottile.utils.ScreenshotHistoryAdapter
import com.github.cvzi.screenshottile.utils.SingleImage
import com.github.cvzi.screenshottile.utils.cleanUpAppData
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.getLocalizedString
import java.io.File
import java.util.Date


/**
 * View recent screenshots especially files in /Android/data folder
 */
class HistoryActivity : BaseAppCompatActivity() {
    companion object {
        const val TAG = "HistoryActivity.kt"
    }

    private lateinit var binding: ActivityHistoryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        binding.setVariable(BR.strings, App.texts)

        binding.buttonClear.setOnClickListener {
            clear()
        }

        binding.switchKeepHistory.setOnCheckedChangeListener { v, isChecked ->
            val prefManager = App.getInstance().prefManager
            if (isChecked != prefManager.keepScreenshotHistory) {
                v.text =
                    getLocalizedString(if (isChecked) R.string.notification_settings_on else R.string.notification_settings_off)
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
            title = getLocalizedString(R.string.button_clear)
            setMessage(formatLocalizedString(R.string.button_clear_confirm, folder))
        }.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            cleanUpAppData(this@HistoryActivity, 0) {
                val adapter =
                    binding.recyclerView.adapter as? ScreenshotHistoryAdapter
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

        val recyclerView: RecyclerView = binding.recyclerView

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager

        val data = loadImageList()

        val adapter = ScreenshotHistoryAdapter(this, data) { record, view ->
            Log.v(TAG, "Click on history item: $record $view")
        }
        recyclerView.adapter = adapter

        binding.switchKeepHistory.isChecked =
            App.getInstance().prefManager.keepScreenshotHistory
        binding.switchKeepHistory.text =
            getLocalizedString(if (binding.switchKeepHistory.isChecked) R.string.notification_settings_on else R.string.notification_settings_off)

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


