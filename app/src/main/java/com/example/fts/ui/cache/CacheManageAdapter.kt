package com.example.fts.ui.cache

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.RootFolder
import com.example.fts.util.DateFormatter
import com.example.fts.util.FileSizeFormatter

class CacheManageAdapter(
    private val onDeleteClick: (RootFolder) -> Unit
) : RecyclerView.Adapter<CacheManageAdapter.CacheViewHolder>() {
    
    private var cacheItems: List<CacheManageViewModel.CacheItem> = emptyList()
    
    inner class CacheViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.cacheName)
        private val infoTextView: TextView = itemView.findViewById(R.id.cacheInfo)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete)
        
        fun bind(cacheItem: CacheManageViewModel.CacheItem) {
            val rootFolder = cacheItem.rootFolder
            nameTextView.text = rootFolder.displayName
            
            val info = buildString {
                append("${rootFolder.entryCount ?: 0} mục")
                rootFolder.lastScannedAt?.let { lastScanned ->
                    append(" • Quét: ${DateFormatter.formatDateTime(lastScanned)}")
                }
                append(" • Size: ${FileSizeFormatter.format(cacheItem.cacheSize)}")
            }
            infoTextView.text = info
            
            deleteButton.setOnClickListener { 
                onDeleteClick(rootFolder) 
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CacheViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cache, parent, false)
        return CacheViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CacheViewHolder, position: Int) {
        holder.bind(cacheItems[position])
    }
    
    override fun getItemCount(): Int = cacheItems.size
    
    fun submitList(newList: List<CacheManageViewModel.CacheItem>) {
        cacheItems = newList
        notifyDataSetChanged()
    }
}