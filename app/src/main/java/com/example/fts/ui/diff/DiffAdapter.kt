package com.example.fts.ui.diff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R

class DiffAdapter : RecyclerView.Adapter<DiffAdapter.DiffViewHolder>() {

    private var items: List<DiffViewModel.DiffItem> = emptyList()

    inner class DiffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tv_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiffViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diff_entry, parent, false)
        return DiffViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiffViewHolder, position: Int) {
        val item = items[position]

        when (item) {
            is DiffViewModel.DiffItem.Header -> {
                holder.textView.text = item.text
                holder.textView.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
                holder.itemView.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_orange_light)
                )
            }
            is DiffViewModel.DiffItem.Added -> {
                holder.textView.text = "+ " + item.path
                holder.textView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            }
            is DiffViewModel.DiffItem.Removed -> {
                holder.textView.text = "- " + item.path
                holder.textView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
            is DiffViewModel.DiffItem.Modified -> {
                holder.textView.text = "~ " + item.path
                holder.textView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_dark))
            }
            is DiffViewModel.DiffItem.Renamed -> {
                holder.textView.text = "R " + item.oldPath + " -> " + item.newPath
                holder.textView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_purple))
            }
            is DiffViewModel.DiffItem.Moved -> {
                holder.textView.text = "M " + item.oldPath + " -> " + item.newPath
                holder.textView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<DiffViewModel.DiffItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}