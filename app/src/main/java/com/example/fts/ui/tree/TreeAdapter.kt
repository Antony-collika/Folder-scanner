package com.example.fts.ui.tree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.domain.flatten.FlatNode
import com.example.fts.util.FileSizeFormatter
import com.example.fts.util.DateFormatter

class TreeAdapter(
    private val onFolderClick: (String) -> Unit,
    private val onFileClick: (com.example.fts.data.model.Entry) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    private var items: List<FlatNode> = emptyList()

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name)
        val metaTextView: TextView = itemView.findViewById(R.id.tv_meta)
        val iconView: View = itemView.findViewById(R.id.iv_icon)
        val expandIcon: View = itemView.findViewById(R.id.iv_expand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_entry, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val item = items[position]
        val entry = item.entry

        holder.nameTextView.text = entry.name

        val iconRes = when (entry.type) {
            com.example.fts.data.model.EntryType.FOLDER -> R.drawable.ic_folder
            else -> R.drawable.ic_file
        }

        val metaParts = mutableListOf<String>()
        if (entry.type == com.example.fts.data.model.EntryType.FOLDER) {
            metaParts.add("Folder")
        } else {
            entry.size?.let { metaParts.add(FileSizeFormatter.format(it)) }
            metaParts.add(DateFormatter.formatDateTime(entry.modified))
        }
        holder.metaTextView.text = metaParts.joinToString(" | ")

        val paddingLeft = 16 * (item.depth + 1)
        holder.itemView.setPadding(paddingLeft, 8, 8, 8)

        if (entry.type == com.example.fts.data.model.EntryType.FOLDER && item.hasChildren) {
            holder.expandIcon.visibility = View.VISIBLE
        } else {
            holder.expandIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (entry.type == com.example.fts.data.model.EntryType.FOLDER) {
                entry.documentId?.let { docId -> onFolderClick(docId) }
            } else {
                onFileClick(entry)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<FlatNode>) {
        items = newItems
        notifyDataSetChanged()
    }
}