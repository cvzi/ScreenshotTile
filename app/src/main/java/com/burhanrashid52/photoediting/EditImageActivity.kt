package com.burhanrashid52.photoediting

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.burhanrashid52.photoediting.EmojiBSFragment.EmojiListener
import com.burhanrashid52.photoediting.base.BaseActivity
import com.burhanrashid52.photoediting.filters.FilterListener
import com.burhanrashid52.photoediting.filters.FilterViewAdapter
import com.burhanrashid52.photoediting.tools.EditingToolsAdapter
import com.burhanrashid52.photoediting.tools.EditingToolsAdapter.OnItemSelected
import com.burhanrashid52.photoediting.tools.ToolType
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.GenericPostActivity
import com.github.cvzi.screenshottile.activities.GenericPostActivity.Companion.OPEN_IMAGE_FROM_URI
import com.github.cvzi.screenshottile.utils.SaveImageHandler
import com.github.cvzi.screenshottile.SaveImageResultSuccess
import com.github.cvzi.screenshottile.utils.SingleImage.Companion.loadBitmapFromDisk
import com.github.cvzi.screenshottile.utils.formatFileName
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ja.burhanrashid52.photoeditor.*
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import java.io.File
import java.io.IOException
import java.util.*


class EditImageActivity : BaseActivity(), OnPhotoEditorListener, View.OnClickListener,
    PropertiesBSFragment.Properties, ShapeBSFragment.Properties, EmojiListener,
    OnItemSelected, FilterListener {

    var mPhotoEditor: PhotoEditor? = null
    private var mPhotoEditorView: PhotoEditorView? = null
    private var mPropertiesBSFragment: PropertiesBSFragment? = null
    private var mShapeBSFragment: ShapeBSFragment? = null
    private var mShapeBuilder: ShapeBuilder? = null
    private var mEmojiBSFragment: EmojiBSFragment? = null
    private var mTxtCurrentTool: TextView? = null
    private var mWonderFont: Typeface? = null
    private var mRvTools: RecyclerView? = null
    private var mRvFilters: RecyclerView? = null
    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private var mRootView: ConstraintLayout? = null
    private val mConstraintSet = ConstraintSet()
    private var mIsFilterVisible = false

    private lateinit var startForPickFolder: ActivityResultLauncher<Intent>
    private val onBackInvokedCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle back button for Android 13+
            OnBackInvokedCallback {
                backPress()
            }
        } else null
    private var onBackInvokedCallbackIsSet = false


    @VisibleForTesting
    var mSaveImageUri: Uri? = null
    private var mSaveFileHelper: FileSaveHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_edit_image)
        initViews()
        if (!handleIntentImage(mPhotoEditorView?.source)) {
            mPhotoEditorView?.source?.setImageResource(android.R.drawable.stat_notify_error)
        }
        mWonderFont = Typeface.createFromAsset(assets, "beyond_wonderland.ttf")
        mPropertiesBSFragment = PropertiesBSFragment()
        mEmojiBSFragment = EmojiBSFragment().apply {
            loadEmoji(this@EditImageActivity)
        }
        mShapeBSFragment = ShapeBSFragment()
        mEmojiBSFragment?.setEmojiListener(this)
        mPropertiesBSFragment?.setPropertiesChangeListener(this)
        mShapeBSFragment?.setPropertiesChangeListener(this)
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools?.layoutManager = llmTools
        mRvTools?.adapter = mEditingToolsAdapter
        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvFilters?.layoutManager = llmFilters
        mRvFilters?.adapter = mFilterViewAdapter

        // NOTE(lucianocheng): Used to set integration testing parameters to PhotoEditor
        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        //Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);
        //Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");
        mPhotoEditor = mPhotoEditorView?.run {
            PhotoEditor.Builder(this@EditImageActivity, this)
                .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build() // build photo editor sdk
        }
        mPhotoEditor?.setOnPhotoEditorListener(this)

        mSaveFileHelper = FileSaveHelper(this)

        startForPickFolder =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        askForFilename(uri)
                    }
                }
            }
    }

    private fun handleIntentImage(source: ImageView?): Boolean {
        if (intent == null) {
            return true
        }
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT, OPEN_IMAGE_FROM_URI -> {
                val uri = intent.data ?: intent.clipData?.getItemAt(0)?.uri ?: return false
                val tryLastBitmap =
                    intent.getBooleanExtra(GenericPostActivity.BITMAP_FROM_LAST_SCREENSHOT, false)
                val lastBitmap = if (tryLastBitmap) {
                    App.getInstance().lastScreenshot
                } else {
                    null
                }
                try {
                    val bitmap = lastBitmap ?: loadBitmapFromDisk(contentResolver, uri, true)
                    source?.setImageBitmap(bitmap)
                    return true
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            ACTION_NO_IMAGE -> {
                return true
            }
            else -> {
                val uri = intent.data ?: intent.clipData?.getItemAt(0)?.uri ?: return false
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    source?.setImageURI(uri)
                    return true
                }
            }
        }
        return false
    }

    private fun initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView)
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRvFilters = findViewById(R.id.rvFilterView)
        mRootView = findViewById(R.id.rootView)

        val imgUndo: ImageView = findViewById(R.id.imgUndo)
        imgUndo.setOnClickListener(this)
        val imgRedo: ImageView = findViewById(R.id.imgRedo)
        imgRedo.setOnClickListener(this)
        val imgCamera: ImageView = findViewById(R.id.imgCamera)
        imgCamera.setOnClickListener(this)
        val imgGallery: ImageView = findViewById(R.id.imgGallery)
        imgGallery.setOnClickListener(this)
        val imgSave: ImageView = findViewById(R.id.imgSave)
        imgSave.setOnClickListener(this)
        val imgSaveAs: ImageView = findViewById(R.id.imgSaveAs)
        imgSaveAs.setOnClickListener(this)
        val imgClose: ImageView = findViewById(R.id.imgClose)
        imgClose.setOnClickListener(this)
        val imgShare: ImageView = findViewById(R.id.imgShare)
        imgShare.setOnClickListener(this)
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment =
            TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String?, colorCode: Int) {
                val styleBuilder = TextStyleBuilder()
                styleBuilder.withTextColor(colorCode)
                if (rootView != null) {
                    mPhotoEditor?.editText(rootView, inputText, styleBuilder)
                }
                mTxtCurrentTool?.setText(R.string.label_text)
            }
        })
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onTouchSourceImage(event: MotionEvent?) {
        Log.d(TAG, "onTouchView() called with: event = [$event]")
    }

    @SuppressLint("NonConstantResourceId", "MissingPermission")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> mPhotoEditor?.undo()
            R.id.imgRedo -> mPhotoEditor?.redo()
            R.id.imgSave -> saveImage()
            R.id.imgSaveAs -> openSaveAsPicker()
            R.id.imgClose -> backPress()
            R.id.imgShare -> shareImage()
            R.id.imgCamera -> {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            }
            R.id.imgGallery -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(
                        intent,
                        getString(R.string.msg_choose_image)
                    ), PICK_REQUEST
                )
            }
        }
    }

    private fun shareImage() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        val saveImageUri = mSaveImageUri
        if (saveImageUri == null) {
            showSnackbar(getString(R.string.msg_save_image_to_share))
            return
        }
        intent.putExtra(Intent.EXTRA_STREAM, buildFileProviderUri(saveImageUri))
        startActivity(Intent.createChooser(intent, getString(R.string.msg_share_image)))
    }

    private fun buildFileProviderUri(uri: Uri): Uri {
        if (FileSaveHelper.isSdkHigherThan28()) {
            return uri
        }
        val path: String = uri.path ?: throw IllegalArgumentException("URI Path Expected")

        return FileProvider.getUriForFile(
            this,
            FILE_PROVIDER_AUTHORITY,
            File(path)
        )
    }


    private fun askForFilename(folder: Uri) {
        val fileNamePattern = App.getInstance().prefManager.fileNamePattern
        val filenameSuggestion = formatFileName(fileNamePattern, Date())

        AlertDialog.Builder(this).apply {
            val editText: EditText

            @SuppressLint("InflateParams")
            val linearLayout: View =
                layoutInflater.inflate(R.layout.dialog_ask_filename, null).apply {
                    editText = findViewById(R.id.editTextFileName)
                    editText.setText(filenameSuggestion)
                    findViewById<TextView>(R.id.textViewFolder).text =
                        folder.path ?: folder.toString()
                }
            setTitle(R.string.dialog_ask_filename_title)
            setMessage(R.string.dialog_ask_filename_hint)
            setView(linearLayout)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                val filename = editText.text.toString()
                dialog.dismiss()
                if (filename.isNotBlank()) {
                    saveImageInFolder(folder, filename.trim())
                }
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun openSaveAsPicker() {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                startForPickFolder.launch(Intent.createChooser(this, "Choose directory"))
            }
        }
    }

    private fun saveImageInFolder(folder: Uri, fileName: String) {
        showLoading("Saving...")
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(true)
            .build()

        mPhotoEditor?.saveAsBitmap(saveSettings, object : OnSaveBitmap {
            override fun onBitmapReady(saveBitmap: Bitmap?) {
                if (saveBitmap == null) {
                    Log.e(TAG, "saveAsBitmap -> onBitmapReady(null)")
                    hideLoading()
                    showSnackbar(getString(R.string.msg_failed_to_save))
                    return
                }

                SaveImageHandler(Looper.getMainLooper()).storeBitmap(
                    this@EditImageActivity,
                    saveBitmap,
                    null,
                    fileName,
                    useAppData = false,
                    directory = folder.toString()
                ) {
                    hideLoading()
                    val result = it as? SaveImageResultSuccess?
                    if (result != null) {
                        hideLoading()
                        showSnackbar(getString(R.string.msg_image_saved))
                        mSaveImageUri = result.uri ?: Uri.fromFile(result.file)
                        mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                    } else {
                        hideLoading()
                        showSnackbar(getString(R.string.msg_failed_to_save))
                        Log.e(
                            TAG,
                            "saveAsBitmap -> storeBitmap -> SaveImageResult Error ${it?.errorMessage}"
                        )
                    }
                }
            }

            override fun onFailure(e: Exception?) {
                hideLoading()
                showSnackbar(getString(R.string.msg_failed_to_save))
                Log.e(TAG, "saveAsBitmap -> onFailure", e)
            }
        })
    }


    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    private fun saveImage() {
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading("Saving...")
            val saveSettings = SaveSettings.Builder()
                .setClearViewsEnabled(true)
                .setTransparencyEnabled(true)
                .build()

            mPhotoEditor?.saveAsBitmap(saveSettings, object : OnSaveBitmap {
                override fun onBitmapReady(saveBitmap: Bitmap?) {
                    if (saveBitmap == null) {
                        Log.e(TAG, "saveAsBitmap -> onBitmapReady(null)")
                        hideLoading()
                        showSnackbar(getString(R.string.msg_failed_to_save))
                        return
                    }

                    SaveImageHandler(Looper.getMainLooper()).storeBitmap(
                        this@EditImageActivity,
                        saveBitmap,
                        null,
                        App.getInstance().prefManager.fileNamePattern,
                        useAppData = false,
                        directory = null
                    ) {
                        hideLoading()
                        val result = it as? SaveImageResultSuccess?
                        if (result != null) {
                            hideLoading()
                            showSnackbar(getString(R.string.msg_image_saved))
                            mSaveImageUri = result.uri ?: Uri.fromFile(result.file)
                            mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                        } else {
                            hideLoading()
                            showSnackbar(getString(R.string.msg_failed_to_save))
                            Log.e(
                                TAG,
                                "saveAsBitmap -> storeBitmap -> SaveImageResult Error ${it?.errorMessage}"
                            )
                        }
                    }
                }

                override fun onFailure(e: Exception?) {
                    hideLoading()
                    showSnackbar(getString(R.string.msg_failed_to_save))
                    Log.e(TAG, "saveAsBitmap -> onFailure", e)
                }
            })
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // TODO(lucianocheng): Replace onActivityResult with Result API from Google
    //                     See https://developer.android.com/training/basics/intents/result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    mPhotoEditor?.clearAllViews()
                    val photo = data?.extras?.get("data") as Bitmap?
                    mPhotoEditorView?.source?.setImageBitmap(photo)
                }
                PICK_REQUEST -> try {
                    mPhotoEditor?.clearAllViews()
                    val uri = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    mPhotoEditorView?.source?.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeColor(colorCode))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeOpacity(opacity))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeSize(shapeSize.toFloat()))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onShapePicked(shapeType: ShapeType?) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeType(shapeType))
    }

    override fun onEmojiClick(emojiUnicode: String?) {
        mPhotoEditor?.addEmoji(emojiUnicode)
        mTxtCurrentTool?.setText(R.string.label_emoji)
    }

    @SuppressLint("MissingPermission")
    override fun isPermissionGranted(isGranted: Boolean, permission: String?) {
        if (isGranted) {
            saveImage()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton(R.string.label_save) { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton(R.string.label_discard) { _: DialogInterface?, _: Int -> finish() }
        builder.create().show()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter?) {
        mPhotoEditor?.setFilterEffect(photoFilter)
    }

    override fun onToolSelected(toolType: ToolType?) {
        addBackButtonHandler()
        when (toolType) {
            ToolType.SHAPE -> {
                mPhotoEditor?.setBrushDrawingMode(true)
                mShapeBuilder = ShapeBuilder()
                mPhotoEditor?.setShape(mShapeBuilder)
                mTxtCurrentTool?.setText(R.string.label_shape)
                showBottomSheetDialogFragment(mShapeBSFragment)
            }
            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object :
                    TextEditorDialogFragment.TextEditorListener {
                    override fun onDone(inputText: String?, colorCode: Int) {
                        val styleBuilder = TextStyleBuilder()
                        styleBuilder.withTextColor(colorCode)
                        mPhotoEditor?.addText(inputText, styleBuilder)
                        mTxtCurrentTool?.setText(R.string.label_text)
                    }
                })
            }
            ToolType.ERASER -> {
                mPhotoEditor?.brushEraser()
                mTxtCurrentTool?.setText(R.string.label_eraser_mode)
            }
            ToolType.FILTER -> {
                mTxtCurrentTool?.setText(R.string.label_filter)
                showFilter(true)
            }
            ToolType.EMOJI -> showBottomSheetDialogFragment(mEmojiBSFragment)
            else -> Log.e(TAG, "onToolSelected() called with unknown: toolType = [$toolType]")
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(mRootView)
        val rvFilterId: Int =
            mRvFilters?.id ?: throw IllegalArgumentException("RV Filter ID Expected")
        if (isVisible) {
            addBackButtonHandler()
            mConstraintSet.clear(rvFilterId, ConstraintSet.START)
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            mConstraintSet.clear(rvFilterId, ConstraintSet.END)
        }
        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        mRootView?.let { TransitionManager.beginDelayedTransition(it, changeBounds) }
        mConstraintSet.applyTo(mRootView)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // This is no longer used on Android 13+/Tiramisu
        // See onBackInvokedCallback for Android 13+
        if (!backPress()) {
            super.onBackPressed()
        }
    }

    private fun backPress(): Boolean {
        val isCacheEmpty =
            mPhotoEditor?.isCacheEmpty ?: throw IllegalArgumentException("isCacheEmpty Expected")

        if (mIsFilterVisible) {
            showFilter(false)
            mTxtCurrentTool?.setText(R.string.app_name)
        } else if (!isCacheEmpty) {
            showSaveDialog()
        } else {
            removeBackButtonHandler()
            return false
        }
        return true
    }

    private fun addBackButtonHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !onBackInvokedCallbackIsSet) {
            onBackInvokedCallback?.let {
                window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback
                )
                onBackInvokedCallbackIsSet = true
            }
        }
    }

    private fun removeBackButtonHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let {
                window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
                onBackInvokedCallbackIsSet = false
            }
        }
    }


    companion object {
        private val TAG = EditImageActivity::class.java.simpleName
        const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"
        private const val CAMERA_REQUEST = 52
        private const val PICK_REQUEST = 53
        const val ACTION_NO_IMAGE = "NO_IMAGE"
        const val ACTION_NEXTGEN_EDIT = "action_nextgen_edit"
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
    }
}