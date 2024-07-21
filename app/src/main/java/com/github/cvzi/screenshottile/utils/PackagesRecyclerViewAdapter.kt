package com.github.cvzi.screenshottile.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.PackageNameFilterMode
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.PackageSelectorListItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Adapter for the RecyclerView of the package selector of the FloatingButtonFilterActivity
 */
@SuppressLint("QueryPermissionsNeeded")
class PackagesRecyclerViewAdapter internal constructor(
    private val context: Activity,
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<PackagesRecyclerViewAdapter.ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var allPackages: List<ResolveInfo>
    private lateinit var displayedPackages: List<ResolveInfo>
    private lateinit var selectedPackages: LinkedHashSet<String>

    private val cacheLabel: HashMap<String, String> = HashMap()
    private val cacheDrawable: HashMap<String, Drawable?> = HashMap()

    private var isBlackList = true

    private val scopeIO = CoroutineScope(Job() + Dispatchers.IO)

    init {
        loadAllApps()
    }

    fun loadAllApps() {
        // Get all activities that are shown in the launcher
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val allLauncherActivities =
            context.packageManager.queryIntentActivities(mainIntent, 0)

        // Get all packages that are installed
        val allApps = context.packageManager.getInstalledPackages(0)
        allApps.forEach { packageInfo ->
            if (allLauncherActivities.none { resolveInfo -> packageInfo.packageName == resolveInfo.activityInfo.packageName }) {
                val resolveInfos = context.packageManager.queryIntentActivities(Intent().apply {
                    setPackage(packageInfo.packageName)
                }, 0)
                if (resolveInfos.isNotEmpty()) {
                    allLauncherActivities.add(resolveInfos[0])
                }
            }
        }
        // Try to get selected packages from the package manager that are not in the above list
        selectedPackages = LinkedHashSet(App.getInstance().prefManager.packageNameFilterList)
        selectedPackages.forEach { selectedPackageName ->
            if (allLauncherActivities.none { resolveInfo -> resolveInfo.activityInfo.packageName == selectedPackageName }) {
                val intent = Intent()
                intent.setPackage(selectedPackageName)
                val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
                if (resolveInfos.isNotEmpty()) {
                    allLauncherActivities.add(resolveInfos[0])
                } else {
                    // Package not found or not accessible -> add a fake ResolveInfo
                    allLauncherActivities.add(
                        ResolveInfo().apply {
                            activityInfo = ActivityInfo().apply {
                                packageName = selectedPackageName
                                name = selectedPackageName
                            }
                        })

                }
            }

        }

        allLauncherActivities.sortBy {
            it.activityInfo.packageName
        }

        displayedPackages = allLauncherActivities.toList()
        allPackages = allLauncherActivities.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(PackageSelectorListItemBinding.inflate(layoutInflater, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.onBind(position)

    override fun getItemCount() = displayedPackages.size

    @SuppressLint("NotifyDataSetChanged")
    fun setFilterMode(filterMode: PackageNameFilterMode) {
        isBlackList = filterMode == PackageNameFilterMode.BLACKLIST
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun search(query: String) {
        if (query.isBlank()) {
            displayedPackages = allPackages
            notifyDataSetChanged()
            return
        }

        displayedPackages = allPackages.filter {
            var label = cacheLabel.getOrElse(it.activityInfo.packageName) {
                try {
                    it.loadLabel(context.packageManager).toString()
                        .ifBlank { it.activityInfo.packageName }
                } catch (e: Exception) {
                    // no-op
                    it.activityInfo.packageName
                }.apply {
                    cacheLabel[it.activityInfo.packageName] = this
                }
            }
            label = label.ifBlank { it.activityInfo.packageName }
            it.activityInfo.packageName.contains(query, ignoreCase = true) ||
                    label.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllVisible(then: (() -> Unit)?) {
        displayedPackages.forEach {
            selectedPackages.add(it.activityInfo.packageName)
        }
        notifyDataSetChanged()
        saveSelectedPackages(then)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun invertSelection(then: (() -> Unit)?) {
        displayedPackages.forEach {
            if (selectedPackages.contains(it.activityInfo.packageName)) {
                selectedPackages.remove(it.activityInfo.packageName)
            } else {
                selectedPackages.add(it.activityInfo.packageName)
            }
        }
        notifyDataSetChanged()
        saveSelectedPackages(then)
    }

    private fun saveSelectedPackages(then: (() -> Unit)? = null) {
        // Store the selected packages in the shared preferences async
        scopeIO.launch(Dispatchers.IO) {
            App.getInstance().prefManager.packageNameFilterList = ArrayList(selectedPackages)
            then?.let {
                context.runOnUiThread {
                    then.invoke()
                }
            }
        }
    }

    inner class ViewHolder internal constructor(private val binding: PackageSelectorListItemBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {
        init {
            this.itemView.setOnClickListener(this@ViewHolder)
        }

        fun onBind(position: Int) {
            val packageManager = this@PackagesRecyclerViewAdapter.context.packageManager
            val resolveInfo = displayedPackages[position]
            val packageName = resolveInfo.activityInfo.packageName

            itemView.setBackgroundResource(
                if (!isBlackList) {
                    R.drawable.package_selector_whitelist_item_background
                } else {
                    R.drawable.package_selector_blacklist_item_background
                }
            )

            binding.textView.text = resolveInfo.activityInfo.name.replace("_", " ")
            itemView.isSelected = selectedPackages.contains(packageName)

            binding.textView.text = cacheLabel.getOrElse(packageName) {
                // Load resources async
                scopeIO.launch(Dispatchers.IO) {
                    var label = packageName
                    try {
                        label = resolveInfo.loadLabel(packageManager).toString().ifBlank {
                            packageName
                        }
                        context.runOnUiThread {
                            binding.textView.text = label.ifBlank {
                                packageName
                            }
                        }
                    } catch (e: Exception) {
                        // no-op
                    } finally {
                        cacheLabel[packageName] = label
                    }
                }
                packageName
            }

            cacheDrawable.getOrElse(packageName) {
                // Load resources async
                scopeIO.launch(Dispatchers.IO) {
                    var drawable: Drawable? = null
                    try {
                        drawable = resolveInfo.loadIcon(packageManager)
                        context.runOnUiThread {
                            if (drawable == null) {
                                binding.imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                            } else {
                                binding.imageView.setImageDrawable(drawable)
                            }
                        }
                    } catch (e: Exception) {
                        // no-op
                    } finally {
                        cacheDrawable[packageName] = drawable
                    }
                }
                null
            }.let {
                if (it == null) {
                    binding.imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                } else {
                    binding.imageView.setImageDrawable(it)
                }
            }

        }

        override fun onClick(view: View) {
            val position = adapterPosition
            if (selectedPackages.contains(displayedPackages[position].activityInfo.packageName)) {
                selectedPackages.remove(displayedPackages[position].activityInfo.packageName)
            } else {
                selectedPackages.add(displayedPackages[position].activityInfo.packageName)
            }
            notifyItemChanged(position)
            saveSelectedPackages {
                clickListener()
            }

        }
    }
}
