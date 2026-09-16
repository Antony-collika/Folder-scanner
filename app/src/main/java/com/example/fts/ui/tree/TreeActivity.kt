package com.example.fts.ui.tree

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.example.fts.R
import com.example.fts.domain.export.JsonExporter
import com.example.fts.domain.export.MarkdownExporter
import com.example.fts.ui.diff.DiffActivity
import com.example.fts.util.*

class TreeActivity : AppCompatActivity() {
    private lateinit var viewModel: TreeViewModel; private lateinit var adapter: TreeAdapter; private lateinit var recyclerView: RecyclerView
    private lateinit var headerRootName: TextView; private lateinit var headerScannedAt: TextView; private lateinit var headerStats: TextView
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri -> uri?.let(viewModel::onExportFileCreated) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_tree); recyclerView = findViewById(R.id.recyclerView); headerRootName = findViewById(R.id.rootName); headerScannedAt = findViewById(R.id.scannedAt); headerStats = findViewById(R.id.stats); setSupportActionBar(findViewById(R.id.toolbar)); supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val searchInput = findViewById<EditText>(R.id.searchInput); val rootUri = intent.getStringExtra(EXTRA_ROOT_URI); val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        if (rootUri == null) { Toast.makeText(this, "Lỗi: Không có thư mục được chọn", Toast.LENGTH_SHORT).show(); finish(); return }
        viewModel = TreeViewModel(rootUri, displayName, application)
        adapter = TreeAdapter({ viewModel.toggleFolder(it) }, { showFileDetail(it) }, { copyPath(it.path) }); recyclerView.layoutManager = LinearLayoutManager(this); recyclerView.adapter = adapter
        viewModel.treeItems.observe(this) { adapter.submitList(it) }; viewModel.snapshot.observe(this) { updateHeader(it) }
        viewModel.showDiff.observe(this) { if (it) startActivity(Intent(this, DiffActivity::class.java).apply { putExtra(EXTRA_ROOT_URI, rootUri); putExtra(EXTRA_DISPLAY_NAME, displayName) }) }
        viewModel.exportResult.observe(this) { result -> result.onSuccess { Toast.makeText(this, "Đã xuất thành công", Toast.LENGTH_SHORT).show() }.onFailure { e -> Toast.makeText(this, "Xuất thất bại: ${e.message}", Toast.LENGTH_SHORT).show() } }
        searchInput.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { viewModel.search(s?.toString().orEmpty()) }; override fun afterTextChanged(s: Editable?) = Unit })
        viewModel.loadData()
    }
    private fun updateHeader(snapshot: com.example.fts.data.model.Snapshot?) { snapshot?.let { supportActionBar?.title = it.rootName; headerRootName.text = it.rootName; headerScannedAt.text = "Quét lúc: ${DateFormatter.formatDateTime(it.scannedAt)}"; headerStats.text = "${it.stats.totalEntries} mục | ${it.stats.totalFolders} folder | ${it.stats.totalFiles} file | ${FileSizeFormatter.format(it.stats.totalSize)}" } }
    private fun showFileDetail(entry: com.example.fts.data.model.Entry) { Toast.makeText(this, "Tên: ${entry.name}\nLoại: ${entry.type}\nĐường dẫn: ${entry.path}\nSửa: ${DateFormatter.formatDateTime(entry.modified)}\nKích thước: ${entry.size?.let(FileSizeFormatter::format) ?: "N/A"}\nMIME: ${entry.mime ?: "N/A"}", Toast.LENGTH_LONG).show() }
    private fun copyPath(path: String) { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Path", path)); Toast.makeText(this, "Đã copy đường dẫn", Toast.LENGTH_SHORT).show() }
    private fun currentMarkdown(): String? = viewModel.snapshot.value?.let { MarkdownExporter.export(it, viewModel.markdownModel(), viewModel.showMetadataInMarkdown()) }
    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.menu_tree, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_export_md -> { currentMarkdown()?.let { viewModel.prepareExport(it, "md"); createDocumentLauncher.launch("tree_${viewModel.displayName}_${System.currentTimeMillis()}.md") }; true }
        R.id.action_export_json -> { viewModel.snapshot.value?.let { viewModel.prepareExport(JsonExporter.export(it), "json"); createDocumentLauncher.launch("tree_${viewModel.displayName}_${System.currentTimeMillis()}.json") }; true }
        R.id.action_copy_md -> { currentMarkdown()?.let { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Markdown", it)); Toast.makeText(this, "Đã copy Markdown", Toast.LENGTH_SHORT).show() }; true }
        R.id.action_share_md -> { currentMarkdown()?.let { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, it) }, "Chia sẻ Markdown")) }; true }
        R.id.action_scan_again -> { viewModel.rescan(); true }
        R.id.action_view_diff -> { viewModel.requestShowDiff(); true }
        else -> super.onOptionsItemSelected(item)
    }
    companion object { const val EXTRA_ROOT_URI = "extra_root_uri"; const val EXTRA_DISPLAY_NAME = "extra_display_name" }
}