package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.R

/*
 * Created by cuzi (cuzi@openmail.cc) on 2020/12/13.
 */

/**
 * Adapter for DrawableListPreference
 */
class DrawableListPreferenceAdapter(
    val context: Context,
    var selectedIndex: Int,
    private val names: Array<String>,
    private val drawables: Array<Int>,
    private val onClick: (Int) -> Unit
) :
    RecyclerView.Adapter<DrawableListPreferenceAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onClick: (Int) -> Unit) :
        RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.itemName)
        val imageView: ImageView = view.findViewById(R.id.itemImage)
        val radioView: RadioButton = view.findViewById(R.id.itemRadio)
        var index: Int = -1

        init {
            view.setOnClickListener {
                if (index > -1) {
                    onClick(index)
                }
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.drawable_list_item, viewGroup, false)

        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = names[position]
        viewHolder.imageView.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                drawables[position]
            )
        )
        viewHolder.index = position
        viewHolder.radioView.isChecked = selectedIndex == position
        if (selectedIndex == position) {
            (viewHolder.imageView.drawable as? Animatable)?.start()
        }
    }

    override fun getItemCount() = names.size

}
