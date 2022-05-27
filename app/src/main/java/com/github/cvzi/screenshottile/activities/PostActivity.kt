package com.github.cvzi.screenshottile.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_RENAME_INPUT
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.*
import java.util.*


class PostActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PostActivity"

        /**
         * New Intent that opens a single image
         *
         * @param context    Context
         * @param uri   Uri   of image
         * @return The intent
         */
        fun newIntentSingleImage(context: Context, uri: Uri): Intent {
            val intent = Intent(context, PostActivity::class.java)
            intent.putExtra(NOTIFICATION_ACTION_RENAME_INPUT, uri.toString())
            return intent
        }

    }

    private var singleImage: SingleImageLoaded? = null
    private var shareIntent: Intent? = null
    private var editIntent: Intent? = null
    private var suggestions: ArrayList<FileNameSuggestion> = ArrayList()

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
        setContentView(R.layout.activity_post)

        intent?.run {
            val imagePath = getStringExtra(NOTIFICATION_ACTION_RENAME_INPUT)
            if (imagePath?.isNotBlank() == true) {
                Uri.parse(imagePath)?.let { imageUri ->
                    SingleImage(imageUri).apply {
                        loadImage(contentResolver, { singleImageLoaded ->
                            runOnUiThread {
                                singleImage = singleImageLoaded
                                showSingleImage(singleImageLoaded)
                            }
                        }, { error ->
                            runOnUiThread {
                                Log.e(TAG, "Failed to load image: $error")
                                findViewById<ImageView>(R.id.imageView).setImageResource(android.R.drawable.stat_notify_error)
                                findViewById<TextView>(R.id.textViewFileName).text =
                                    "Failed to load image"
                            }
                        })
                    }
                }
            }
        }

        findViewById<Button>(R.id.buttonShare).setOnClickListener {
            shareIntent?.let { intent ->
                openIntent(intent)
            }
        }
        findViewById<Button>(R.id.buttonEdit).setOnClickListener {
            editIntent?.let { intent ->
                openIntent(intent)
            }
        }
        findViewById<Button>(R.id.buttonDelete).setOnClickListener {
            singleImage?.let { singleImageLoaded ->
                if (deleteImage(this, singleImageLoaded.uri)) {
                    toastMessage(
                        R.string.screenshot_deleted,
                        ToastType.ACTIVITY,
                        Toast.LENGTH_SHORT
                    )
                    // Show delete icon and close activity
                    findViewById<ImageView>(R.id.imageView).setImageResource(android.R.drawable.ic_menu_delete)
                    findViewById<TextView>(R.id.textViewFileName).setText(R.string.screenshot_deleted)
                    it.postDelayed({
                        finish()
                    }, 1000L)
                } else {
                    toastMessage(
                        R.string.screenshot_delete_failed,
                        ToastType.ACTIVITY
                    )
                }
            }
        }
        findViewById<Button>(R.id.buttonRename).setOnClickListener {
            singleImage?.let { singleImageLoaded ->
                rename(singleImageLoaded)
            }
        }
        findViewById<EditText>(R.id.editTextNewName).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                singleImage?.let { singleImageLoaded ->
                    rename(singleImageLoaded)
                }
            }
            return@setOnEditorActionListener false
        }

        findViewById<RecyclerView>(R.id.recyclerViewSuggestions).apply {
            layoutManager = LinearLayoutManager(context)
            suggestions.clear()
            suggestions.addAll(App.getInstance().prefManager.getFileNameSuggestions())
            adapter = SuggestionsAdapter(suggestions).apply {
                onTextClickListener = { _: View, index: Int ->
                    this@PostActivity.findViewById<EditText>(R.id.editTextNewName)
                        .setText(suggestions[index].value)
                }
                onDeleteClickListener = { _: View, index: Int ->
                    App.getInstance().prefManager.run {
                        removeFileName(suggestions[index])
                        updateData(getFileNameSuggestions())
                        invalidate()
                    }
                }
                onStarClickListener = { _: View, index: Int ->
                    App.getInstance().prefManager.run {
                        removeFileName(suggestions[index])
                        addStarredFileName(suggestions[index].value)
                        updateData(getFileNameSuggestions())
                    }
                }
            }
        }
        findViewById<ImageButton>(R.id.imageButtonSaveSuggestion).setOnClickListener {
            saveNewSuggestions()
        }
        findViewById<EditText>(R.id.editTextAddSuggestion).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                saveNewSuggestions()
            }
            return@setOnEditorActionListener false
        }

        findViewById<TextView>(R.id.textViewDateIso).setOnClickListener {
            findViewById<EditText>(R.id.editTextNewName)
                .setText((it as TextView).text)
        }
        findViewById<TextView>(R.id.textViewDateLocal).setOnClickListener {
            findViewById<EditText>(R.id.editTextNewName)
                .setText((it as TextView).text)
        }

    }

    private fun saveNewSuggestions() {
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

    private fun openIntent(intent: Intent) {
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Log.e(TAG, "resolveActivity(editIntent) returned null")
        }
    }


    private fun showSingleImage(singleImage: SingleImageLoaded) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(singleImage.thumbnail)
        findViewById<TextView>(R.id.textViewFileName).text = singleImage.fileName
        findViewById<EditText>(R.id.editTextNewName).setText(singleImage.fileName)
        shareIntent = shareImageChooserIntent(this, singleImage.uri, singleImage.mimeType)
        editIntent = editImageChooserIntent(this, singleImage.uri, singleImage.mimeType)
        findViewById<TextView>(R.id.textViewDateIso).text =
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.getDefault()
            ).format(singleImage.lastModified)
        findViewById<TextView>(R.id.textViewDateLocal).text =
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault())
                .format(singleImage.lastModified)

    }


    private fun rename(singleImageLoaded: SingleImageLoaded) {
        val newName = findViewById<EditText>(R.id.editTextNewName).text.toString()
        if (newName.isBlank()) {
            return
        }
        if (newName == singleImageLoaded.fileName) {
            toastMessage(R.string.post_rename_error_identical, ToastType.ACTIVITY)
            return
        }
        App.getInstance().prefManager.addRecentFileName(newName)
        val result = renameImage(this, singleImageLoaded.uri, newName)
        if (result.first) {
            toastMessage(getString(R.string.screenshot_renamed, newName), ToastType.ACTIVITY)
            finish()
            result.second?.let {
                startActivity(newIntentSingleImage(this, it))
            }
        } else {
            toastMessage(R.string.screenshot_rename_failed, ToastType.ACTIVITY)
        }
    }

}
