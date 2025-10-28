package com.github.cvzi.screenshottile.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.animation.AlphaAnimation
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityLanguageBinding
import com.github.cvzi.screenshottile.utils.Language
import com.github.cvzi.screenshottile.utils.LanguageAdapter
import com.github.cvzi.screenshottile.utils.setUserLanguage
import org.xmlpull.v1.XmlPullParser

class LanguageActivity : BaseAppCompatActivity() {
    companion object {
        const val TAG = "LanguageActivity.kt"

        /**
         * Start activity
         */
        fun start(ctx: Context) = ctx.startActivity(Intent(ctx, LanguageActivity::class.java))
    }

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var allLanguagesAdapter: LanguageAdapter
    private lateinit var selectedLanguagesAdapter: LanguageAdapter

    private val selectedLanguages = mutableListOf<Language>()
    private val allLanguages = mutableListOf<Language>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_language)
        binding.setVariable(BR.strings, App.texts)

        Log.v(TAG, "Current languages: ${Resources.getSystem().configuration.locales}")

        getLanguagesFromXml(allLanguages)

        allLanguagesAdapter = LanguageAdapter(allLanguages) { language ->
            if (!selectedLanguages.contains(language)) {
                selectedLanguages.add(language)
                selectedLanguagesAdapter.notifyItemInserted(selectedLanguages.size - 1)
                highlightApplyButton()
            }
        }

        selectedLanguagesAdapter = LanguageAdapter(selectedLanguages) { language ->
            val index = selectedLanguages.indexOf(language)
            selectedLanguages.remove(language)
            selectedLanguagesAdapter.notifyItemRemoved(index)
            highlightApplyButton()
        }

        val allLanguagesRecyclerView: RecyclerView = binding.allLanguagesRecyclerView
        allLanguagesRecyclerView.layoutManager = LinearLayoutManager(this)
        allLanguagesRecyclerView.adapter = allLanguagesAdapter

        val selectedLanguagesRecyclerView: RecyclerView = binding.selectedLanguagesRecyclerView
        selectedLanguagesRecyclerView.layoutManager = LinearLayoutManager(this)
        selectedLanguagesRecyclerView.adapter = selectedLanguagesAdapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition
                selectedLanguages.add(toPosition, selectedLanguages.removeAt(fromPosition))
                selectedLanguagesAdapter.notifyItemMoved(fromPosition, toPosition)
                highlightApplyButton()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                selectedLanguages.removeAt(viewHolder.absoluteAdapterPosition)
                selectedLanguagesAdapter.notifyItemRemoved(viewHolder.absoluteAdapterPosition)
                highlightApplyButton()
            }
        })
        itemTouchHelper.attachToRecyclerView(selectedLanguagesRecyclerView)

        binding.buttonApply.setOnClickListener {
            applySelectedLanguages()
        }

        binding.buttonReset.setOnClickListener {
            resetLanguages()
        }

        binding.buttonMoreSettings.setOnClickListener {
            SettingsActivity.start(this)
        }

    }


    override fun onResume() {
        Log.v(TAG, "onResume!")
        super.onResume()

        val userLanguages = App.getInstance().prefManager.userLanguages
        Log.v(TAG, "onResume: User languages: $userLanguages")
        if (userLanguages?.isNotBlank() == true) {
            userLanguages.split(",").mapNotNull { short -> allLanguages.find { it.short == short } }
                .forEach { language ->
                    if (!selectedLanguages.contains(language)) {
                        selectedLanguages.add(language)
                        selectedLanguagesAdapter.notifyItemInserted(selectedLanguages.size - 1)
                    }
                }
        }
    }


    @SuppressLint("DiscouragedApi")
    private fun getLanguagesFromXml(allLanguages: MutableList<Language>) {
        val skipLanguages = listOf("in", "iw")
        allLanguages.clear()
        val parser = resources.getXml(R.xml.locales_config)
        parser.next()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                val localeShort =
                    parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                if (localeShort != null && !skipLanguages.contains(localeShort)) {
                    val fullNameId = resources.getIdentifier(
                        "language_name_${localeShort.replace("-", "_")}",
                        "string",
                        packageName
                    )
                    val fullName = if (fullNameId == 0) {
                        Log.e(TAG, "No full name for language code '$localeShort'")
                        localeShort
                    } else {
                        getString(fullNameId)
                    }
                    val progressId = resources.getIdentifier(
                        "language_progress_${
                            localeShort.replace("-", "_")
                        }", "string", packageName
                    )
                    val progress = if (progressId == 0) {
                        Log.e(TAG, "No progress status for language code '$localeShort'")
                        "Status unknown"
                    } else {
                        getString(progressId)
                    }
                    allLanguages.add(Language(localeShort, fullName, progress))
                }
            }
            eventType = parser.next()
        }
    }

    private fun highlightApplyButton() {
        binding.buttonApply.apply {
            isEnabled = true
            postDelayed({
                AlphaAnimation(0.6f, 1.0f).apply {
                    duration = 800
                    repeatCount = 4
                    repeatMode = AlphaAnimation.REVERSE
                    startAnimation(this)
                }
            }, 700)
        }
    }

    private fun applySelectedLanguages() {
        Log.v(TAG, "Selected languages: ${selectedLanguagesAdapter.languages}")
        App.getInstance().prefManager.userLanguages =
            if (selectedLanguagesAdapter.languages.isNotEmpty()) {
                selectedLanguagesAdapter.languages.joinToString(",") { it.short }
            } else {
                null
            }
        setUserLanguage(force=true)
        recreate()
    }

    private fun resetLanguages() {
        App.getInstance().prefManager.userLanguages = null
        setUserLanguage(force=true)
        recreate()
    }

}

