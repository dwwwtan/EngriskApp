package com.dex.engrisk.vocabulary.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.databinding.ItemTopicBinding
import com.dex.engrisk.model.Topic

class TopicAdapter(
    private val topics: List<Topic>,
    private val onTopicClicked: (Topic) -> Unit
) : RecyclerView.Adapter<TopicAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTopicBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topics[position]
        holder.binding.tvTopicName.text = topic.name
        holder.binding.ivTopicIcon.setImageResource(topic.iconResId)

        holder.itemView.setOnClickListener {
            onTopicClicked(topic)
        }
    }

    override fun getItemCount(): Int = topics.size
}