package com.arnavpaul.smartcalc

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val items: List<Message>) : RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.msg_text)
        var pulse: ValueAnimator? = null
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position].role == "user") 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == 1) R.layout.item_message_user else R.layout.item_message
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.pulse?.cancel()
        holder.pulse = null
        holder.tv.alpha = 1f

        if (m.role == "user") {
            holder.tv.text = m.content
        } else if (m.content.isEmpty()) {
            holder.tv.text = "\u2022\u2022\u2022"
            ValueAnimator.ofFloat(0.25f, 1f).apply {
                duration = 480
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { holder.tv.alpha = it.animatedValue as Float }
                holder.pulse = this
                start()
            }
            return
        } else {
            holder.tv.text = m.content
        }

        holder.tv.translationY = 18f
        holder.tv.alpha = 0f
        holder.tv.animate().translationY(0f).alpha(1f).setDuration(240).start()
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        holder.pulse?.cancel()
        holder.pulse = null
        super.onViewDetachedFromWindow(holder)
    }
}
