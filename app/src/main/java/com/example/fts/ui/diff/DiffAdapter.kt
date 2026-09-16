package com.example.fts.ui.diff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.example.fts.R

class DiffAdapter : RecyclerView.Adapter<DiffAdapter.DiffViewHolder>() {
    private var items: List<DiffViewModel.DiffItem> = emptyList()

    inner class DiffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tv_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiffViewHolder =
        DiffViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_diff_entry, parent, false))

    override fun onBindViewHolder(holder: DiffViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is DiffViewModel.DiffItem.Header -> {
                holder.textView.text = item.text
                holder.textView.textSize = 20f
            }
            is DiffViewModel.DiffItem.Added -> holder.textView.text = "+ ${item.path}"
            is DiffViewModel.DiffItem.Removed -> holder.textView.text = "- ${item.path}"
            is DiffViewModel.DiffItem.Modified -> holder.textView.text = "~ ${item.path}"
            is DiffViewModel.DiffItem.Renamed -> holder.textView.text = "R ${item.oldPath} -> ${item.newPath}"
            is DiffViewModel.DiffItem.Moved -> holder.textView.text = "M ${item.oldPath} -> ${item.newPath}"
        }
    }

    override fun getItemCount(): Int = items.size
    fun submitList(newItems: List<DiffViewModel.DiffItem>) { items = newItems; notifyDataSetChanged() }
}