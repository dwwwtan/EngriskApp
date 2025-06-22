package com.dex.engrisk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.databinding.ItemTopicBinding

class TopicAdapter(
    private val topics: List<String>,
    private val onTopicClicked: (String) -> Unit
) : RecyclerView.Adapter<TopicAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTopicBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topics[position]
        holder.binding.tvTopicName.text = topic
        holder.itemView.setOnClickListener {
            onTopicClicked(topic)
        }
    }

    override fun getItemCount(): Int = topics.size
}