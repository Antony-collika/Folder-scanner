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

    private var items: List<RootFolder> = emptyList()

    inner class RootFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        val countTextView: TextView = itemView.findViewById(R.id.tv_count)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        val scanButton: View = itemView.findViewById(R.id.btn_scan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RootFolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_root_folder, parent, false)
        return RootFolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: RootFolderViewHolder, position: Int) {
        val item = items[position]

        holder.nameTextView.text = item.displayName
        holder.countTextView.text = item.entryCount?.let { it.toString() + " entries" } ?: "0 entries"
        holder.dateTextView.text = item.lastScannedAt?.let {
            DateFormatter.formatDateTime(it)
        } ?: "Never scanned"

        holder.itemView.setOnClickListener { onClick(item) }
        holder.scanButton.setOnClickListener { onScanClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<RootFolder>) {
        items = newItems
        notifyDataSetChanged()
    }
}