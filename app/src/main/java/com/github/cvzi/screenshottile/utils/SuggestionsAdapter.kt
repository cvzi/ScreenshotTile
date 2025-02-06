package com.github.cvzi.screenshottile.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.RecentFolderItemBinding
import com.github.cvzi.screenshottile.databinding.SuggestionItemBinding

typealias OnItemClickListener = (v: View, index: Int) -> Unit

/**
 * A single file name suggestion
 */
class FileNameSuggestion(val value: String, val starred: Boolean, val dataIndex: Int) {
    override fun toString(): String {
        return "FileNameSuggestion('$value' ${if (starred) "starred" else "recent"}#$dataIndex)"
    }
}


/**
 * SuggestionsAdapter
 */
class SuggestionsAdapter(private var data: ArrayList<FileNameSuggestion>) :
    RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

    var onTextClickListener: OnItemClickListener? = null
    var onDeleteClickListener: OnItemClickListener? = null
    var onStarClickListener: OnItemClickListener? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: Array<FileNameSuggestion>) {
        this.data.clear()
        this.data.addAll(newData)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val starButton: ImageButton
        private val deleteButton: ImageButton

        init {
            textView = view.findViewById(R.id.textView)
            starButton = view.findViewById(R.id.imageButtonStar)
            deleteButton = view.findViewById(R.id.imageButtonDelete)
            textView.setOnClickListener {
                if (adapterPosition == layoutPosition && adapterPosition != -1) {
                    onTextClickListener?.invoke(it, adapterPosition)
                }
            }
            starButton.setOnClickListener {
                if (adapterPosition == layoutPosition && adapterPosition != -1) {
                    onStarClickListener?.invoke(it, adapterPosition)
                }
            }
            deleteButton.setOnClickListener {
                if (adapterPosition == layoutPosition && adapterPosition != -1) {
                    onDeleteClickListener?.invoke(it, adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = DataBindingUtil.inflate<SuggestionItemBinding>(LayoutInflater.from(viewGroup.context), R.layout.suggestion_item, viewGroup, false)
        itemBinding .setVariable(BR.strings, App.texts)
        return ViewHolder(itemBinding.root)

    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = data[position].value
        if (data[position].starred) {
            viewHolder.starButton.setBackgroundResource(android.R.drawable.btn_star_big_on)
        } else {
            viewHolder.starButton.setBackgroundResource(android.R.drawable.btn_star_big_off)
        }
    }

    override fun getItemCount() = data.size
}