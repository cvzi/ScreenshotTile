package com.github.cvzi.screenshottile.utils

import android.icu.text.DateFormat
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.activities.HistoryActivity
import com.github.cvzi.screenshottile.activities.PostActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

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
                        App.getInstance().prefManager.screenshotHistoryRemove(it.uri)
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
                            App.getInstance().prefManager.screenshotHistoryRemove(currentSingleImage.uri)
                            App.getInstance().prefManager.screenshotHistoryRemove(file)
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