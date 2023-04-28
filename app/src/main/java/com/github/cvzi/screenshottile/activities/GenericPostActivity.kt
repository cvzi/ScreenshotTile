package com.github.cvzi.screenshottile.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.*
import com.github.cvzi.screenshottile.activities.PostActivity.Companion.newIntentSingleImage
import com.github.cvzi.screenshottile.utils.*
import java.util.*


open class GenericPostActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "GenericPostActivity"
        const val OPEN_IMAGE_FROM_URI = "com.github.cvzi.screenshottile.OPEN_IMAGE_FROM_URI"
        const val BITMAP_FROM_LAST_SCREENSHOT =
            "com.github.cvzi.screenshottile.BITMAP_FROM_LAST_SCREENSHOT"
        const val PARENT_FOLDER_URI = "com.github.cvzi.screenshottile.PARENT_FOLDER_URI"
        const val HIGHLIGHT = "com.github.cvzi.screenshottile.HIGHLIGHT"
        const val HIGHLIGHT_FILENAME = 1
        const val HIGHLIGHT_FOLDER = 2
    }

    var singleImage: SingleImageLoaded? = null
    var shareIntent: Intent? = null
    var editIntent: Intent? = null
    var photoEditorIntent: Intent? = null
    var cropIntent: Intent? = null
    var suggestions: ArrayList<FileNameSuggestion> = ArrayList()
    protected var savedInstanceState: Bundle? = null

    /**
     * If the activity is already open, we need to update the intent,
     * otherwise getIntent() returns the old intent in onCreate()
     **/
    override fun onNewIntent(intent: Intent?) {
        setIntent(intent)
        super.onNewIntent(intent)
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun saveNewSuggestions() {
        val editText = findViewById<EditText>(R.id.editTextAddSuggestion)
        val name = editText.text.toString()
        if (name.isNotBlank()) {
            App.getInstance().prefManager.addStarredFileName(name)
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSuggestions)
            (recyclerView.adapter as? SuggestionsAdapter)?.apply {
                updateData(App.getInstance().prefManager.getFileNameSuggestions())
            }
            editText.setText("")
        }
    }

    fun openIntent(intent: Intent) {
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Log.e(TAG, "resolveActivity(editIntent) returned null")
        }
    }


    fun showSingleImage(singleImage: SingleImageLoaded) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(singleImage.bitmap)
        findViewById<TextView>(R.id.textViewFileName).text = singleImage.fileName
        findViewById<TextView>(R.id.textViewFileSize).text = singleImage.size?.let {
            android.text.format.Formatter.formatShortFileSize(this, it)
        } ?: ""
        findViewById<TextView>(R.id.textViewFileFolder).text =
            singleImage.folder ?: singleImage.file?.parentFile?.absolutePath ?: singleImage.uri.path
                    ?: ""
        findViewById<EditText>(R.id.editTextNewName).setText(singleImage.fileName)
        shareIntent = shareImageChooserIntent(this, singleImage.uri, singleImage.mimeType)
        editIntent = editImageChooserIntent(this, singleImage.uri, singleImage.mimeType)
        cropIntent =
            PostCropActivity.newIntentSingleImage(this, singleImage.uri, singleImage.mimeType)
        photoEditorIntent = Intent(this, EditImageActivity::class.java).apply {
            action = Intent.ACTION_EDIT
            setDataAndTypeAndNormalize(singleImage.uri, singleImage.mimeType)
        }

        findViewById<TextView>(R.id.textViewDateIso).text = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()
        ).format(singleImage.lastModified)
        findViewById<TextView>(R.id.textViewDateLocal).text =
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault())
                .format(singleImage.lastModified)

        restoreSavedInstanceValues()
    }


    fun rename(singleImageLoaded: SingleImageLoaded) {
        val newName = findViewById<EditText>(R.id.editTextNewName).text.toString()
        if (newName.isBlank()) {
            return
        }
        if (newName == singleImageLoaded.fileName) {
            toastMessage(R.string.post_rename_error_identical, ToastType.ACTIVITY)
            return
        }
        App.getInstance().prefManager.addRecentFileName(newName)

        val result = if (singleImageLoaded.parentFolderUri != null) {
            // Image is already in another folder, so we need to use moveImage otherwise
            // the image would be moved back to the default folder
            moveImage(
                this,
                singleImageLoaded.uri,
                destFolderUri = singleImageLoaded.parentFolderUri,
                newName = newName
            )
        } else {
            renameImage(this, singleImageLoaded.uri, newName)
        }

        if (result.first) {
            toastMessage(getString(R.string.screenshot_renamed, newName), ToastType.ACTIVITY)
            finish()
            result.second?.let {
                startActivity(
                    newIntentSingleImage(
                        this,
                        it,
                        parentFolderUri = singleImageLoaded.parentFolderUri,
                        highlight = HIGHLIGHT_FILENAME
                    )
                )
            }
        } else {
            toastMessage(R.string.screenshot_rename_failed, ToastType.ACTIVITY)
        }
    }

    fun move(singleImageLoaded: SingleImageLoaded, destFolderUri: Uri) {
        var newName: String = findViewById<EditText>(R.id.editTextNewName).text.toString().trim()
        if (newName.isBlank()) {
            newName = singleImageLoaded.fileName
        }

        val result = try {
            moveImage(this, singleImageLoaded.uri, destFolderUri = destFolderUri, newName = newName)
        } catch (e: Exception) {
            Log.e(TAG, "moveImage failed with Exception:", e)
            null
        }
        if (result?.first == true) {
            App.getInstance().prefManager.addRecentFolder(destFolderUri)
            toastMessage("Moved to\n${niceFullPathFromUri(destFolderUri)}", ToastType.ACTIVITY)
            finish()
            result.second?.let {
                startActivity(
                    newIntentSingleImage(
                        this, it, parentFolderUri = destFolderUri, highlight = HIGHLIGHT_FOLDER
                    )
                )
            }
        } else {
            toastMessage("Failed to move file", ToastType.ACTIVITY)
        }
    }

    protected open fun restoreSavedInstanceValues() {
    }
}
