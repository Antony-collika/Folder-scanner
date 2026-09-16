package com.example.fts.ui.tree

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.MarkdownModel
import com.example.fts.domain.export.JsonExporter
import com.example.fts.domain.export.MarkdownExporter
import com.example.fts.ui.diff.DiffActivity

class TreeActivity : AppCompatActivity() {
    private lateinit var viewModel: TreeViewModel
    private lateinit var adapter: TreeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerRootName: TextView
    private lateinit var headerScannedAt: TextView
    private lateinit var headerStats: TextView

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        uri?.let { viewModel.onExportFileCreated(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree)
        recyclerView = findViewById(R.id.recyclerView)
        headerRootName = findViewById(R.id.rootName)
        headerScannedAt = findViewById(R.id.scannedAt)
        headerStats = findViewById(R.id.stats)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val rootUri = intent.getStringExtra(EXTRA_ROOT_URI)
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        if (rootUri == null) {
            Toast.makeText(this, "Error: No folder selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = TreeViewModel(rootUri, displayName, application)
        adapter = TreeAdapter(
            onFolderClick = { documentId -> viewModel.toggleFolder(documentId) },
            onFileClick = { entry -> showFileDetail(entry) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        viewModel.treeItems.observe(this) { adapter.submitList(it) }
        viewModel.snapshot.observe(this) { updateHeader(it) }
        viewModel.showDiff.observe(this) { show ->
            if (show) startActivity(Intent(this, DiffActivity::class.java).apply {
                putExtra(EXTRA_ROOT_URI, rootUri)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
            })
        }
        viewModel.exportResult.observe(this) { result ->
            result.onSuccess { Toast.makeText(this, "Exported successfully", Toast.LENGTH_SHORT).show() }
                .onFailure { e -> Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
        viewModel.loadData()
    }

    private fun updateHeader(snapshot: com.example.fts.data.model.Snapshot?) {
        snapshot?.let {
            supportActionBar?.title = it.rootName
            headerRootName.text = it.rootName
            headerScannedAt.text = "Scanned: " + com.example.fts.util.DateFormatter.formatDateTime(it.scannedAt)
            headerStats.text = "${it.stats.totalEntries} entries | ${it.stats.totalFolders} folders | ${it.stats.totalFiles} files | ${com.example.fts.util.FileSizeFormatter.format(it.stats.totalSize)}"
        }
    }

    private fun showFileDetail(entry: com.example.fts.data.model.Entry) {
        val sizeText = entry.size?.let { com.example.fts.util.FileSizeFormatter.format(it) } ?: "N/A"
        val mimeText = entry.mime ?: "N/A"
        val detail = "Name: ${entry.name}\nType: ${entry.type}\nPath: ${entry.path}\nModified: ${com.example.fts.util.DateFormatter.formatDateTime(entry.modified)}\nSize: $sizeText\nMIME: $mimeText"
        Toast.makeText(this, detail, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tree, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_export_md -> {
            val markdown = viewModel.snapshot.value?.let { MarkdownExporter.export(it, MarkdownModel.D) } ?: ""
            viewModel.prepareExport(markdown, "md")
            createDocumentLauncher.launch("tree_${viewModel.displayName}_${System.currentTimeMillis()}.md")
            true
        }
        R.id.action_export_json -> {
            val json = viewModel.snapshot.value?.let { JsonExporter.export(it) } ?: ""
            viewModel.prepareExport(json, "json")
            createDocumentLauncher.launch("tree_${viewModel.displayName}_${System.currentTimeMillis()}.json")
            true
        }
        R.id.action_scan_again -> { viewModel.rescan(); true }
        R.id.action_view_diff -> { viewModel.requestShowDiff(); true }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_ROOT_URI = "extra_root_uri"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
    }
}