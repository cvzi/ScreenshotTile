package com.github.cvzi.screenshottile.utils

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.ListPreference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.R

/*
 * Created by cuzi (cuzi@openmail.cc) on 2020/12/13.
 */

/**
 * Preference to choose from a list of named drawables in a dialog
 */
class DrawableListPreference : ListPreference {
    @Suppress("unused")
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        @Suppress("unused") defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr
    )
    @Suppress("unused")
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    @Suppress("unused")
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    @Suppress("unused")
    constructor(context: Context?) : super(context)

    private var selectedShutterIndex: Int = 0
    override fun onClick() {
        val shutterCollection = ShutterCollection(context, R.array.shutters, R.array.shutter_names)
        selectedShutterIndex = value.toIntOrNull() ?: 0
        val builder = AlertDialog.Builder(context)
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            value = selectedShutterIndex.toString()
            shutterCollection.index = selectedShutterIndex
            summary = shutterCollection.current().name
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val dialogLayout: View = inflater.inflate(R.layout.drawable_list_preference, null)
        dialog.setView(dialogLayout)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setOnShowListener {
            val constraintLayout = dialog.findViewById<ConstraintLayout>(R.id.constraintLayout)
            val recyclerView = RecyclerView(context)
            recyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            val adapter = DrawableListPreferenceAdapter(
                context,
                selectedShutterIndex,
                shutterCollection.getNames(),
                shutterCollection.getNormals()
            ) { index ->
                (recyclerView.adapter as? DrawableListPreferenceAdapter)?.selectedIndex = index
                recyclerView.adapter?.notifyDataSetChanged()
                selectedShutterIndex = index
            }
            recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
            recyclerView.invalidate()
            constraintLayout.addView(recyclerView)
        }
        dialog.show()
    }
}
