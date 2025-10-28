package com.github.cvzi.screenshottile.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.RecentFolderItemBinding
import androidx.core.net.toUri

/**
 * A single recent folder item
 */
class RecentFolder(private val value: String, val dataIndex: Int) {
    val uri: Uri
        get() = value.toUri()

    override fun toString(): String {
        return "RecentFolder('$value' #$dataIndex)"
    }
}


/**
 * RecentFoldersAdapter
 */
class RecentFoldersAdapter(
    private var context: Context,
    private var data: ArrayList<RecentFolder>
) :
    RecyclerView.Adapter<RecentFoldersAdapter.ViewHolder>() {

    var onTextClickListener: OnItemClickListener? = null
    var onDeleteClickListener: OnItemClickListener? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: Array<RecentFolder>) {
        this.data.clear()
        this.data.addAll(newData.filter {
            val docDir = DocumentFile.fromTreeUri(context, it.uri)
            docDir?.canWrite() == true
        })
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)

        init {
            val deleteButton: ImageButton = view.findViewById(R.id.imageButtonDelete)
            val imageView: View = view.findViewById(R.id.imageView)
            textView.setOnClickListener {
                if (absoluteAdapterPosition == layoutPosition && absoluteAdapterPosition != -1) {
                    onTextClickListener?.invoke(it, absoluteAdapterPosition)
                }
            }
            imageView.setOnClickListener {
                if (absoluteAdapterPosition == layoutPosition && absoluteAdapterPosition != -1) {
                    onTextClickListener?.invoke(it, absoluteAdapterPosition)
                }
            }
            deleteButton.setOnClickListener {
                if (absoluteAdapterPosition == layoutPosition && absoluteAdapterPosition != -1) {
                    onDeleteClickListener?.invoke(it, absoluteAdapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = DataBindingUtil.inflate<RecentFolderItemBinding>(LayoutInflater.from(viewGroup.context), R.layout.recent_folder_item, viewGroup, false)
        itemBinding .setVariable(BR.strings, App.texts)
        return ViewHolder(itemBinding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = niceFullPathFromUri(data[position].uri)
    }

    override fun getItemCount() = data.size
}