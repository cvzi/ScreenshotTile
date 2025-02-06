package com.github.cvzi.screenshottile.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.cvzi.screenshottile.databinding.LanguageItemBinding

data class Language(val short: String, val long: String, val progress: String)

class LanguageAdapter(
    val languages: List<Language>, private val onClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding =
            LanguageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        holder.bind(language, onClick)
    }

    override fun getItemCount(): Int = languages.size

    class LanguageViewHolder(val binding: LanguageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(language: Language, onClick: (Language) -> Unit) {
            binding.textViewLong.text = language.long
            binding.textViewShort.text = language.short
            binding.textViewProgress.text = language.progress
            itemView.setOnClickListener { onClick(language) }
        }
    }
}
