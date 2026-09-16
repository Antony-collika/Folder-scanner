package com.example.fts.ui.tree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.domain.flatten.FlatNode
import com.example.fts.util.FileSizeFormatter
import com.example.fts.util.MarkdownEscape

class TreeAdapter(
    private val onFolderClick: (String) -> Unit,
    private val onFileClick: (Entry) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {
    
    private var treeItems: List<FlatNode> = emptyList()
    
    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.icon)
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val sizeTextView: TextView = itemView.findViewById(R.id.size)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        
        fun bind(flatNode: FlatNode) {
            val entry = flatNode.entry
            
            // Icon
            val iconRes = when (entry.type) {
                EntryType.FOLDER -> R.drawable.ic_folder
                EntryType.FILE -> R.drawable.ic_file
            }
            iconImageView.setImageResource(iconRes)
            
            // Name
            nameTextView.text = MarkdownEscape.escape(entry.name)
            
            // Size (only for files)
            if (entry.type == EntryType.FILE && entry.size != null) {
                sizeTextView.text = FileSizeFormatter.format(entry.size)
                sizeTextView.visibility = View.VISIBLE
            } else {
                sizeTextView.visibility = View.GONE
            }
            
            // Expand icon (only for folders with children)
            if (entry.type == EntryType.FOLDER && flatNode.hasChildren) {
                val expandIconRes = if (flatNode.isExpanded) {
                    R.drawable.ic_expand_less
                } else {
                    R.drawable.ic_expand_more
                }
                expandIcon.setImageResource(expandIconRes)
                expandIcon.visibility = View.VISIBLE
            } else {
                expandIcon.visibility = View.GONE
            }
            
            // Indent based on depth
            val indent = flatNode.depth * 32
            itemView.setPadding(indent, 0, 0, 0)
            
            // Click listeners
            itemView.setOnClickListener {
                if (entry.type == EntryType.FOLDER && flatNode.hasChildren) {
                    onFolderClick(entry.documentId)
                } else if (entry.type == EntryType.FILE) {
                    onFileClick(entry)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_entry, parent, false)
        return TreeViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        holder.bind(treeItems[position])
    }
    
    override fun getItemCount(): Int = treeItems.size
    
    fun submitList(newList: List<FlatNode>) {
        treeItems = newList
        notifyDataSetChanged()
    }
}