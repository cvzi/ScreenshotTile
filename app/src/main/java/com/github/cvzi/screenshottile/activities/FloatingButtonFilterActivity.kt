package com.github.cvzi.screenshottile.activities


import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.PackageNameFilterMode
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityFloatingButtonFilterBinding
import com.github.cvzi.screenshottile.databinding.ActivityMainBinding
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.PackagesRecyclerViewAdapter


/**
 * Change settings of the floating button
 */
@RequiresApi(Build.VERSION_CODES.P)
class FloatingButtonFilterActivity : BaseAppCompatActivity() {
    companion object {
        const val TAG = "FloatingButtonFilter"

        /**
         * Get intent
         */
        fun newIntent(ctx: Context): Intent =
            Intent(ctx, FloatingButtonFilterActivity::class.java)

        /**
         * Start activity
         */
        fun start(ctx: Context) = ctx.startActivity(newIntent(ctx))

        /**
         * Start activity from service
         */
        fun startNewTask(ctx: Context) = ctx.startActivity(newIntent(ctx).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private var savedInstanceState: Bundle? = null
    private val prefManager = App.getInstance().prefManager

    private lateinit var packagesRecyclerViewAdapter: PackagesRecyclerViewAdapter

    private lateinit var binding: ActivityFloatingButtonFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityFloatingButtonFilterBinding>(this, R.layout.activity_floating_button_filter)
        binding.setVariable(BR.strings, App.texts)


        binding.buttonMoreSettings.setOnClickListener {
            SettingsActivity.start(this)
        }
        binding.buttonFloatingButtonSettings.setOnClickListener {
            FloatingButtonSettingsActivity.start(this)
        }
        binding.switchFloatingButtonEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButton = isChecked
            if (isChecked && ScreenshotAccessibilityService.instance == null) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, MainActivity.TAG)
            } else {
                ScreenshotAccessibilityService.instance?.updateFloatingButton()
            }
        }
        binding.switchFloatingFilterEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefManager.packageNameFilterEnabled = isChecked

            for (child in binding.radioGroupFilterMode.children) {
                if (child is RadioButton) {
                    child.isEnabled = isChecked
                }
            }
            ScreenshotAccessibilityService.instance?.updatePackageFilter()
        }

        binding.radioGroupFilterMode.setOnCheckedChangeListener { group, checkedId ->
            val button =
                group.findViewById<RadioButton?>(checkedId) ?: return@setOnCheckedChangeListener
            val filterMode = PackageNameFilterMode.fromString(button.tag.toString())
                ?: return@setOnCheckedChangeListener
            (binding.packagesRecyclerView.adapter as? PackagesRecyclerViewAdapter)?.setFilterMode(
                filterMode
            )
            prefManager.packageNameFilterMode = filterMode
            ScreenshotAccessibilityService.instance?.updatePackageFilter()
        }

        binding.packagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FloatingButtonFilterActivity)
            packagesRecyclerViewAdapter =
                PackagesRecyclerViewAdapter(this@FloatingButtonFilterActivity) {
                    binding.switchFloatingFilterEnabled.isChecked = true
                    ScreenshotAccessibilityService.instance?.updatePackageFilter()
                }
            adapter = packagesRecyclerViewAdapter
        }

        binding.editTextSearch.addTextChangedListener {
            packagesRecyclerViewAdapter.search(it.toString())
        }

        binding.buttonSelectAll.setOnClickListener {
            packagesRecyclerViewAdapter.selectAllVisible {
                ScreenshotAccessibilityService.instance?.updatePackageFilter()
            }

        }

        binding.buttonInvertSelection.setOnClickListener {
            packagesRecyclerViewAdapter.invertSelection {
                ScreenshotAccessibilityService.instance?.updatePackageFilter()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.switchFloatingButtonEnabled.isChecked =
            ScreenshotAccessibilityService.instance != null && prefManager.floatingButton


        val packageNameFilterEnabled = prefManager.packageNameFilterEnabled
        binding.switchFloatingFilterEnabled.isChecked = packageNameFilterEnabled

        for (child in binding.radioGroupFilterMode.children) {
            if (child is RadioButton) {
                val filterMode = PackageNameFilterMode.fromString(child.tag.toString())
                child.isChecked = filterMode == prefManager.packageNameFilterMode
                child.isEnabled = packageNameFilterEnabled
            }
        }

        packagesRecyclerViewAdapter.loadAllApps()
        packagesRecyclerViewAdapter.setFilterMode(prefManager.packageNameFilterMode)
    }

    override fun onPause() {
        super.onPause()
        ScreenshotAccessibilityService.instance?.updateFloatingButton(forceRedraw = true)
    }


}


