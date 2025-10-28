package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.databinding.ToneSelectorListItemBinding


/**
 * Adapter for the RecyclerView of the sound/tone selector in the PostSettingsActivity.
 */
class TonesRecyclerViewAdapter internal constructor(
    context: Context,
    tonesHashMap: HashMap<String, Int>,
    selectedToneName: String,
    private val clickListener: (view: View, position: Int, name: String) -> Unit
) : RecyclerView.Adapter<TonesRecyclerViewAdapter.ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var toneNames: List<String> = tonesHashMap.keys.toList().sorted()
    var selectedIndex = toneNames.indexOf(selectedToneName)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ToneSelectorListItemBinding.inflate(layoutInflater, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.onBind(position)

    override fun getItemCount() = toneNames.size

    inner class ViewHolder internal constructor(private val binding: ToneSelectorListItemBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {
        init {
            this.itemView.setOnClickListener(this@ViewHolder)
        }

        fun onBind(position: Int) {
            binding.textView.text = toneNames[position].replace("_", " ")
            itemView.isSelected = selectedIndex == position
        }

        override fun onClick(view: View) {
            val oldSelectedIndex = selectedIndex
            selectedIndex = absoluteAdapterPosition
            clickListener(view, absoluteAdapterPosition, toneNames[absoluteAdapterPosition])
            notifyItemChanged(oldSelectedIndex)
            notifyItemChanged(selectedIndex)
        }
    }
}
