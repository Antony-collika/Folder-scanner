package com.example.fts.ui.tree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.MarkdownModel
import com.example.fts.domain.flatten.FlatNode
import com.example.fts.util.FileSizeFormatter

class TreeAdapter(
    private var model: MarkdownModel = MarkdownModel.D,
    private var showMetadata: Boolean = false,
    private val onFolderClick: (String) -> Unit,
    private val onFileClick: (com.example.fts.data.model.Entry) -> Unit,
    private val onLongClick: (com.example.fts.data.model.Entry) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {
    private var treeItems: List<FlatNode> = emptyList()

    fun configure(model: MarkdownModel, showMetadata: Boolean) {
        this.model = model
        this.showMetadata = showMetadata
        notifyDataSetChanged()
    }

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val prefix: TextView = itemView.findViewById(R.id.prefix)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val size: TextView = itemView.findViewById(R.id.size)

        fun bind(node: FlatNode, position: Int) {
            val entry = node.entry
            icon.setImageResource(if (entry.type == EntryType.FOLDER) R.drawable.ic_folder else R.drawable.ic_file)
            val suffix = if (entry.type == EntryType.FOLDER && model != MarkdownModel.A && model != MarkdownModel.B) "/" else ""
            name.text = entry.name + suffix

            prefix.text = when (model) {
                MarkdownModel.A -> "#".repeat((node.depth + 1).coerceAtMost(6)) + " "
                MarkdownModel.B -> if (entry.type == EntryType.FOLDER) "#".repeat((node.depth + 1).coerceAtMost(6)) + " " else ""
                MarkdownModel.C -> "  ".repeat(node.depth) + "- "
                MarkdownModel.D -> asciiPrefix(position, node.depth)
            }
            prefix.visibility = if (model == MarkdownModel.D || model == MarkdownModel.C || model == MarkdownModel.A || model == MarkdownModel.B) View.VISIBLE else View.GONE

            if (entry.type == EntryType.FILE && entry.size != null) {
                size.text = FileSizeFormatter.format(entry.size)
                size.visibility = View.VISIBLE
            } else size.visibility = View.GONE
            if (showMetadata) {
                itemView.contentDescription = "${entry.name}, ${entry.path}, ${entry.size?.let(FileSizeFormatter::format) ?: "folder"}"
            } else itemView.contentDescription = entry.name

            if (entry.type == EntryType.FOLDER && node.hasChildren) {
                expandIcon.setImageResource(if (node.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                expandIcon.visibility = View.VISIBLE
            } else expandIcon.visibility = View.INVISIBLE

            itemView.setOnClickListener {
                if (entry.type == EntryType.FOLDER && node.hasChildren) onFolderClick(entry.documentId)
                else if (entry.type == EntryType.FILE) onFileClick(entry)
            }
            itemView.setOnLongClickListener { onLongClick(entry); true }
        }

        private fun asciiPrefix(position: Int, depth: Int): String {
            if (depth == 0) return ""
            val parts = mutableListOf<String>()
            var childDepth = depth
            for (i in position - 1 downTo 0) {
                val previous = treeItems[i]
                if (previous.depth < childDepth) {
                    val nextDepth = treeItems.getOrNull(i + 1)?.depth ?: -1
                    parts.add(if (previous.depth < childDepth - 1 && nextDepth > previous.depth) "│   " else "    ")
                    childDepth = previous.depth
                    if (childDepth == 0) break
                }
            }
            val indentation = parts.asReversed().joinToString("")
            val isLast = position == treeItems.lastIndex || treeItems[position + 1].depth <= depth
            return indentation + if (isLast) "└── " else "├── "
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder =
        TreeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tree_entry, parent, false))
    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) = holder.bind(treeItems[position], position)
    override fun getItemCount(): Int = treeItems.size
    fun submitList(newList: List<FlatNode>) { treeItems = newList; notifyDataSetChanged() }
}
