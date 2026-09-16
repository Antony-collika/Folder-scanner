package com.example.fts.ui.diff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R

class DiffAdapter : RecyclerView.Adapter<DiffAdapter.DiffViewHolder>() {
    
    private var diffItems: List<DiffViewModel.DiffItem> = emptyList()
    
    inner class DiffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.text)
        
        fun bind(item: DiffViewModel.DiffItem) {
            when (item) {
                is DiffViewModel.DiffItem.Header -> {
                    textView.text = item.text
                    textView.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
                }
                is DiffViewModel.DiffItem.Added -> {
                    textView.text = "+ ${item.path}"
                    textView.setTextColor(itemView.context.getColor(R.color.added_color))
                }
                is DiffViewModel.DiffItem.Removed -> {
                    textView.text = "- ${item.path}"
                    textView.setTextColor(itemView.context.getColor(R.color.removed_color))
                }
                is DiffViewModel.DiffItem.Modified -> {
                    val changesText = item.changes.entries.joinToString(", ") { (key, pair) ->
                        "$key: ${pair.first} → ${pair.second}"
                    }
                    textView.text = "~ ${item.path} ($changesText)"
                    textView.setTextColor(itemView.context.getColor(R.color.modified_color))
                }
                is DiffViewModel.DiffItem.Renamed -> {
                    textView.text = "↳ ${item.oldPath} → ${item.newPath}"
                    textView.setTextColor(itemView.context.getColor(R.color.renamed_color))
                }
                is DiffViewModel.DiffItem.Moved -> {
                    textView.text = "» ${item.oldPath} → ${item.newPath}"
                    textView.setTextColor(itemView.context.getColor(R.color.moved_color))
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiffViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diff, parent, false)
        return DiffViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DiffViewHolder, position: Int) {
        holder.bind(diffItems[position])
    }
    
    override fun getItemCount(): Int = diffItems.size
    
    fun submitList(newList: List<DiffViewModel.DiffItem>) {
        diffItems = newList
        notifyDataSetChanged()
    }
}