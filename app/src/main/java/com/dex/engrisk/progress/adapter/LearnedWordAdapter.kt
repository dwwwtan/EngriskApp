package com.dex.engrisk.progress.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.databinding.ItemLearnedWordBinding
import com.dex.engrisk.model.Vocabulary

class LearnedWordAdapter(private var words: List<Vocabulary>) :
    RecyclerView.Adapter<LearnedWordAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLearnedWordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLearnedWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = words[position]
        holder.binding.tvWord.text = word.word
        holder.binding.tvDefinition.text = word.definition
    }

    override fun getItemCount(): Int = words.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newWords: List<Vocabulary>) {
        words = newWords
        notifyDataSetChanged()
    }
}