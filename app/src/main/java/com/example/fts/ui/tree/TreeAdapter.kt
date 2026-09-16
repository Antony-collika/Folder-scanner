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

class TreeAdapter(
    private val onFolderClick: (String) -> Unit,
    private val onFileClick: (Entry) -> Unit,
    private val onLongClick: (Entry) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {
    private var treeItems: List<FlatNode> = emptyList()

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.icon)
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val sizeTextView: TextView = itemView.findViewById(R.id.size)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)

        fun bind(flatNode: FlatNode) {
            val entry = flatNode.entry
            iconImageView.setImageResource(if (entry.type == EntryType.FOLDER) R.drawable.ic_folder else R.drawable.ic_file)
            nameTextView.text = entry.name
            if (entry.type == EntryType.FILE && entry.size != null) {
                sizeTextView.text = FileSizeFormatter.format(entry.size)
                sizeTextView.visibility = View.VISIBLE
            } else sizeTextView.visibility = View.GONE

            if (entry.type == EntryType.FOLDER && flatNode.hasChildren) {
                expandIcon.setImageResource(if (flatNode.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                expandIcon.visibility = View.VISIBLE
            } else expandIcon.visibility = View.GONE

            itemView.setPadding(flatNode.depth * 32, 0, 0, 0)
            itemView.setOnClickListener {
                if (entry.type == EntryType.FOLDER && flatNode.hasChildren) onFolderClick(entry.documentId)
                else if (entry.type == EntryType.FILE) onFileClick(entry)
            }
            itemView.setOnLongClickListener {
                onLongClick(entry)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder =
        TreeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tree_entry, parent, false))

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) = holder.bind(treeItems[position])
    override fun getItemCount(): Int = treeItems.size
    fun submitList(newList: List<FlatNode>) { treeItems = newList; notifyDataSetChanged() }
}