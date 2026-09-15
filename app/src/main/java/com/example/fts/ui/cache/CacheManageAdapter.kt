package com.example.fts.ui.cache

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.RootFolder
import com.example.fts.util.FileSizeFormatter
import com.example.fts.util.DateFormatter

class CacheManageAdapter(
    private val onDeleteClick: (RootFolder) -> Unit
) : RecyclerView.Adapter<CacheManageAdapter.CacheViewHolder>() {

    private var items: List<CacheManageViewModel.CacheItem> = emptyList()

    inner class CacheViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        val sizeTextView: TextView = itemView.findViewById(R.id.tv_size)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        val deleteButton: View = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CacheViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cache_entry, parent, false)
        return CacheViewHolder(view)
    }

    override fun onBindViewHolder(holder: CacheViewHolder, position: Int) {
        val item = items[position]

        holder.nameTextView.text = item.rootFolder.displayName
        holder.sizeTextView.text = FileSizeFormatter.format(item.cacheSize)
        holder.dateTextView.text = item.rootFolder.lastScannedAt?.let {
            DateFormatter.formatDateTime(it)
        } ?: "Never"

        holder.deleteButton.setOnClickListener {
            onDeleteClick(item.rootFolder)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<CacheManageViewModel.CacheItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}