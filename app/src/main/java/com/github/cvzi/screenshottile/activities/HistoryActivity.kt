package com.github.cvzi.screenshottile.activities


import android.annotation.SuppressLint
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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


class ScreenshotHistoryAdapter(
    val activity: HistoryActivity,
    var dataSet: MutableList<SingleImage> = ArrayList(),
    private val onClick: (SingleImage, View) -> Unit
) :
    RecyclerView.Adapter<ScreenshotHistoryAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "ScreenshotHistoryAdptr"
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    inner class ViewHolder(
        private var view: ViewGroup,
        private var lifecycleOwner: LifecycleOwner,
        private val onClick: (SingleImage, View) -> Unit
    ) :
        RecyclerView.ViewHolder(view), LifecycleOwner {
        val textViewFileName: TextView
        val textViewDate: TextView
        val textViewFolder: TextView
        val imageView: ImageView
        private val buttonDelete: Button
        private val buttonDetails: Button
        val buttonMove: Button
        var linearLayout: LinearLayout? = null

        private var currentRecord: SingleImage? = null
        private val lifecycleRegistry = LifecycleRegistry(this)
        private var paused: Boolean = false

        init {
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
            // Define click listener for the ViewHolder's View.
            textViewFileName = view.findViewById(R.id.textViewFileName)
            textViewDate = view.findViewById(R.id.textViewDate)
            textViewFolder = view.findViewById(R.id.textViewFolder)
            imageView = view.findViewById(R.id.imageView)
            buttonDelete = view.findViewById(R.id.buttonDelete)
            buttonDetails = view.findViewById(R.id.buttonDetails)
            buttonMove = view.findViewById(R.id.buttonMove)
            textViewFileName.setOnClickListener {
                currentRecord?.let {
                    onClick(it, view)
                }
            }
            imageView.setOnClickListener {
                currentRecord?.let {
                    onClick(it, view)
                }
            }
            buttonDelete.setOnClickListener {
                currentRecord?.let {
                    if (deleteImage(activity, it.uri)) {
                        activity.toastMessage(
                            R.string.screenshot_deleted,
                            ToastType.SUCCESS,
                            Toast.LENGTH_SHORT
                        )
                        removeFromData(it)
                    } else {
                        activity.toastMessage(
                            R.string.screenshot_delete_failed,
                            ToastType.ERROR
                        )
                    }
                }
            }
            buttonDetails.setOnClickListener {
                currentRecord?.let {
                    Log.v(TAG, "buttonDetails click: $it")
                    activity.startActivity(
                        PostActivity.newIntentSingleImage(activity, it.uri)
                    )
                }
            }
            buttonMove.setOnClickListener {
                currentRecord?.let { currentSingleImage ->
                    val file = currentSingleImage.file
                    if (file != null) {
                        val result = moveImageToStorage(activity, file, currentSingleImage.fileName)
                        val uri = result.second
                        if (result.first && uri != null) {
                            var index = -1
                            dataSet.forEachIndexed { i, singleImage ->
                                if (singleImage.uri == currentSingleImage.uri) {
                                    index = i
                                }
                            }
                            if (index > -1) {
                                dataSet[index] = SingleImage(uri, null, null, Date(), false)
                                notifyItemChanged(index)
                            }
                        }
                    }
                }
            }

            view.setOnClickListener {
                currentRecord?.let {
                    onClick(it, view)
                }
            }

            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun bind(wallpaperImageRecord: SingleImage) {
            lifecycleOwner = this@ScreenshotHistoryAdapter.activity

            currentRecord = wallpaperImageRecord
        }

        fun attachToWindow() {
            if (paused) {
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                paused = false
            } else {
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
            }
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }

    }

    private fun removeFromData(toRemove: SingleImage) {
        // Remove from recycler view
        val iterator = dataSet.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val singleImage = iterator.next()
            if (singleImage.uri == toRemove.uri) {
                iterator.remove()
                notifyItemRemoved(index)
            } else {
                index += 1
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.history_item, viewGroup, false)
        return ViewHolder(view as ViewGroup, activity, onClick)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        var item = dataSet[position]

        var name: String = if (item.uri.lastPathSegment.isNullOrBlank()) {
            item.uri.toString()
        } else {
            item.uri.lastPathSegment
                ?: item.uri.toString()
        }
        if (name.contains(":")) {
            name = name.split(":").last()
        }

        viewHolder.textViewFileName.text = name

        viewHolder.buttonMove.visibility = if (item.isAppData) {
            View.VISIBLE
        } else {
            View.GONE
        }

        var folder: String = item.folder
            ?: item.file?.parent
            ?: item.uri.toString()

        if (folder.contains("//")) {
            folder = folder.split("//").last()
        }
        if (folder.contains(".fileprovider/")) {
            folder = folder.split(".fileprovider/").last()
        }
        viewHolder.textViewFolder.text = folder

        viewHolder.bind(item)

        CoroutineScope(Job() + Dispatchers.IO).launch(Dispatchers.IO) {
            if (item.bitmap == null) {
                try {
                    item = item.loadImage(
                        this@ScreenshotHistoryAdapter.activity.contentResolver,
                        Size(200, 400)
                    )
                    if (dataSet[position].uri == item.uri) {
                        // Make sure it's still the same image at this position before overwriting it
                        dataSet[position] = item
                    }
                } catch (e: Exception) {
                    Log.w(TAG, e.stackTraceToString())
                }
            }

            viewHolder.imageView.post {
                val bitmap = item.bitmap
                if (bitmap != null) {
                    viewHolder.imageView.layoutParams =
                        LinearLayout.LayoutParams(viewHolder.imageView.layoutParams)
                            .apply {
                                width = bitmap.width
                                height = bitmap.height
                            }
                    viewHolder.imageView.setImageBitmap(bitmap)
                } else {
                    viewHolder.imageView.layoutParams =
                        LinearLayout.LayoutParams(viewHolder.imageView.layoutParams)
                            .apply {
                                width = 48
                                height = 48
                            }
                    viewHolder.imageView.setImageResource(android.R.drawable.presence_busy)

                    // Remove image that was not loaded
                    viewHolder.imageView.postDelayed({
                        App.getInstance().prefManager.screenshotHistoryRemove(item.uri)
                        removeFromData(item)
                    }, 1000L)


                }

                viewHolder.textViewFileName.text = item.fileName ?: name

                val dateFormat = DateFormat.getDateTimeInstance(
                    DateFormat.LONG,
                    DateFormat.MEDIUM,
                    Locale.getDefault()
                )
                viewHolder.textViewDate.text = if (item.file != null) {
                    try {
                        item.file?.lastModified()?.run {
                            dateFormat.format(Date(this))
                        } ?: ""
                    } catch (e: Exception) {
                        Log.e(TAG, e.stackTraceToString())
                        ""
                    }
                } else if (item.lastModified != null) {
                    dateFormat.format(item.lastModified)
                } else {
                    ""
                }


            }
        }

    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.attachToWindow()
    }

    override fun getItemCount() = dataSet.size

}

