package com.example.fts.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.RootFolder
import com.example.fts.util.DateFormatter

class RootFolderAdapter(
    private val onClick: (RootFolder) -> Unit,
    private val onScanClick: (RootFolder) -> Unit
) : RecyclerView.Adapter<RootFolderAdapter.RootFolderViewHolder>() {
    
    private var rootFolders: List<RootFolder> = emptyList()
    
    inner class RootFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.folderName)
        private val infoTextView: TextView = itemView.findViewById(R.id.folderInfo)
        private val scanButton: View = itemView.findViewById(R.id.btn_scan)
        
        fun bind(rootFolder: RootFolder) {
            itemView.setOnClickListener { onClick(rootFolder) }
            scanButton.setOnClickListener { onScanClick(rootFolder) }
            
            nameTextView.text = rootFolder.displayName
            
            val info = buildString {
                append("${rootFolder.entryCount ?: 0} mục")
                rootFolder.lastScannedAt?.let { lastScanned ->
                    append(" • Quét lần cuối: ${DateFormatter.formatDateTime(lastScanned)}")
                }
            }
            infoTextView.text = info
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RootFolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_root_folder, parent, false)
        return RootFolderViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RootFolderViewHolder, position: Int) {
        holder.bind(rootFolders[position])
    }
    
    override fun getItemCount(): Int = rootFolders.size
    
    fun submitList(newList: List<RootFolder>) {
        rootFolders = newList
        notifyDataSetChanged()
    }
}