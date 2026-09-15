package com.example.fts.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.RootFolder
import com.example.fts.util.DateFormatter

class RootFolderAdapter(private val onClick: (RootFolder) -> Unit) : 
    RecyclerView.Adapter<RootFolderAdapter.RootFolderViewHolder>() {
    
    private var folders: List<RootFolder> = emptyList()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RootFolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_root_folder, parent, false)
        return RootFolderViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RootFolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }
    
    override fun getItemCount(): Int = folders.size
    
    fun submitList(newFolders: List<RootFolder>) {
        folders = newFolders
        notifyDataSetChanged()
    }
    
    inner class RootFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.textViewName)
        private val countView: TextView = itemView.findViewById(R.id.textViewCount)
        private val dateView: TextView = itemView.findViewById(R.id.textViewDate)
        
        fun bind(folder: RootFolder) {
            nameView.text = folder.displayName
            countView.text = folder.entryCount?.toString() + " muc" ?: "Chua quet"
            dateView.text = folder.lastScannedAt?.let { DateFormatter.format(it) } ?: "Chua quet"
            itemView.setOnClickListener { onClick(folder) }
        }
    }

}