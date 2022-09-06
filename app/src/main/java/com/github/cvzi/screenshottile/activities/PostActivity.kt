package com.github.cvzi.screenshottile.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_RENAME_INPUT
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.*


class PostActivity : GenericPostActivity() {
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

        fun newIntentSingleImageBitmap(context: Context, uri: Uri): Intent {
            val intent = Intent(context, PostActivity::class.java)
            intent.putExtra(NOTIFICATION_ACTION_RENAME_INPUT, uri.toString())
            intent.putExtra(BITMAP_FROM_LAST_SCREENSHOT, true)
            return intent
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        intent?.run {
            val imagePath = getStringExtra(NOTIFICATION_ACTION_RENAME_INPUT)
            val tryLastBitmap = getBooleanExtra(BITMAP_FROM_LAST_SCREENSHOT, false)
            val lastBitmap = if (tryLastBitmap) {
                App.getInstance().lastScreenshot
            } else {
                null
            }
            if (imagePath?.isNotBlank() == true) {
                Uri.parse(imagePath)?.let { imageUri ->
                    SingleImage(imageUri).apply {
                        loadImageInThread(
                            contentResolver,
                            Size(200, 400),
                            lastBitmap,
                            { singleImageLoaded ->
                                runOnUiThread {
                                    singleImage = singleImageLoaded
                                    showSingleImage(singleImageLoaded)
                                }
                            },
                            { error ->
                                runOnUiThread {
                                    Log.e(TAG, "Failed to load image: $error")
                                    findViewById<ImageView>(R.id.imageView).setImageResource(android.R.drawable.stat_notify_error)
                                    findViewById<TextView>(R.id.textViewFileName).text =
                                        "Failed to load image"
                                    findViewById<TextView>(R.id.textViewFileSize).text = ""
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
                    findViewById<TextView>(R.id.textViewFileSize).text = "0"
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

    override fun onRestoreInstanceState(mSavedInstanceState: Bundle) {
        super.onRestoreInstanceState(mSavedInstanceState)
        savedInstanceState = mSavedInstanceState
    }

    override fun restoreSavedInstanceValues() {
        savedInstanceState?.run {
            getString("editText_${R.id.editTextNewName}", null)?.let {
                findViewById<EditText>(R.id.editTextNewName).setText(it)
            }
            getString("editText_${R.id.editTextAddSuggestion}", null)?.let {
                findViewById<EditText>(R.id.editTextAddSuggestion).setText(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "editText_${R.id.editTextNewName}",
            findViewById<EditText>(R.id.editTextNewName)?.text.toString()
        )
        outState.putString(
            "editText_${R.id.editTextAddSuggestion}",
            findViewById<EditText>(R.id.editTextAddSuggestion)?.text.toString()
        )
        super.onSaveInstanceState(outState)
    }
}
