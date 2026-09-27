package com.arnavpaul.smartcalc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val items: List<Message>) : RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.msg_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.tv.text = when {
            m.role == "user" -> "You: " + m.content
            m.content.isEmpty() -> "AI: thinking..."
            else -> "AI: " + m.content
        }
    }
}
