package com.example.fts.ui.tree

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fts.R
import com.example.fts.data.model.Entry
import com.example.fts.domain.export.JsonExporter
import com.example.fts.domain.export.MarkdownExporter
import com.example.fts.ui.diff.DiffActivity
import com.example.fts.util.DateFormatter
import com.example.fts.util.FileSizeFormatter

class TreeActivity : AppCompatActivity() {
    private lateinit var viewModel: TreeViewModel
    private lateinit var adapter: TreeAdapter
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var levelContainer: LinearLayout
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri -> uri?.let(viewModel::onExportFileCreated) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree)
        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)
        levelContainer = findViewById(R.id.levelContainer)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val searchInput = findViewById<EditText>(R.id.searchInput)
        val rootUri = intent.getStringExtra(EXTRA_ROOT_URI)
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        if (rootUri == null) { Toast.makeText(this, "Không có thư mục được chọn", Toast.LENGTH_SHORT).show(); finish(); return }

        viewModel = TreeViewModel(rootUri, displayName, application)
        adapter = TreeAdapter(viewModel.markdownModel(), viewModel.showMetadataInMarkdown(), { viewModel.toggleFolder(it) }, { showFileDetail(it) }, { copyPath(it.path) })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        findViewById<Button>(R.id.sortButton).setOnClickListener { showSortDialog() }
        findViewById<Button>(R.id.exportButton).setOnClickListener { exportMarkdown() }
        findViewById<Button>(R.id.exportJsonButton).setOnClickListener { exportJson() }

        viewModel.treeItems.observe(this) { adapter.submitList(it); emptyState.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE; recyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE }
        viewModel.snapshot.observe(this) { snapshot ->
            snapshot ?: return@observe
            supportActionBar?.title = snapshot.rootName
            findViewById<TextView>(R.id.rootName).text = snapshot.rootName
            findViewById<TextView>(R.id.scannedAt).text = "Quét lúc: ${DateFormatter.formatDateTime(snapshot.scannedAt)}"
            findViewById<TextView>(R.id.stats).text = "${snapshot.stats.totalEntries} mục  •  ${snapshot.stats.totalFolders} folder  •  ${snapshot.stats.totalFiles} file  •  ${FileSizeFormatter.format(snapshot.stats.totalSize)}"
            renderLevelControls()
        }
        viewModel.level.observe(this) { renderLevelControls() }
        viewModel.showDiff.observe(this) { if (it) openDiff(rootUri, displayName) }
        viewModel.exportResult.observe(this) { result -> result.onSuccess { Toast.makeText(this, "Đã xuất thành công", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(this, "Xuất thất bại: ${it.message}", Toast.LENGTH_SHORT).show() } }
        viewModel.isLoading.observe(this) { loading -> findViewById<Button>(R.id.exportButton).isEnabled = !loading; findViewById<Button>(R.id.exportJsonButton).isEnabled = !loading; findViewById<Button>(R.id.sortButton).isEnabled = !loading }
        searchInput.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { viewModel.search(s?.toString().orEmpty()) }; override fun afterTextChanged(s: Editable?) = Unit })
        viewModel.loadData()
    }

    private fun renderLevelControls() {
        if (!::viewModel.isInitialized || !::levelContainer.isInitialized) return
        val maxLevel = viewModel.maxLevel()
        val selected = viewModel.level.value ?: 1
        while (levelContainer.childCount > 1) levelContainer.removeViewAt(1)

        for (level in 1..6) {
            val isSelected = level <= selected
            val isAvailable = level <= maxLevel
            val icon = TextView(this).apply {
                text = if (isSelected) "🔷" else "🔶"
                textSize = 22f
                gravity = Gravity.CENTER
                minWidth = 58
                minHeight = 56
                setPadding(12, 4, 12, 4)
                isClickable = isAvailable
                isFocusable = isAvailable
                alpha = when { !isAvailable -> 0.35f; isSelected -> 1f; else -> 0.72f }
                contentDescription = if (isAvailable) "Hiển thị đến cấp $level" else "Cấp $level không có trong cây"
                if (isAvailable) setOnClickListener { viewModel.setExpandLevel(level) }
            }
            levelContainer.addView(icon)
        }

        val expandAll = TextView(this).apply {
            text = if (selected >= maxLevel) "🔆" else "🔅"
            textSize = 22f
            gravity = Gravity.CENTER
            minWidth = 62
            minHeight = 56
            setPadding(12, 4, 12, 4)
            isClickable = true
            isFocusable = true
            typeface = Typeface.DEFAULT_BOLD
            contentDescription = if (selected >= maxLevel) "Đã mở tất cả" else "Mở tất cả"
            setOnClickListener { viewModel.expandAll() }
        }
        levelContainer.addView(expandAll)
    }

    private fun showSortDialog() { val labels = arrayOf("Tên (folder trước, A-Z)", "Ngày sửa (mới nhất)", "Kích thước (lớn nhất)", "Loại (folder trước)"); val values = arrayOf("name", "date", "size", "type"); val current = values.indexOf(viewModel.sortOrder()).coerceAtLeast(0); AlertDialog.Builder(this).setTitle("Sắp xếp cây").setSingleChoiceItems(labels, current) { dialog, which -> viewModel.setSortOrder(values[which]); dialog.dismiss() }.show() }
    private fun exportMarkdown() { currentMarkdown()?.let { viewModel.prepareExport(it, "md"); createDocumentLauncher.launch("tree_${viewModel.displayName}_${System.currentTimeMillis()}.md") } }
    private fun exportJson() { viewModel.snapshot.value?.let { viewModel.prepareExport(JsonExporter.export(it), "json"); createDocumentLauncher.launch("tree_${viewModel.displayName}_${System.currentTimeMillis()}.json") } }
    private fun currentMarkdown(): String? = viewModel.snapshot.value?.let { MarkdownExporter.export(it, viewModel.markdownModel(), viewModel.showMetadataInMarkdown(), viewModel.sortOrder()) }
    private fun showFileDetail(entry: Entry) { val size = entry.size?.let(FileSizeFormatter::format) ?: "N/A"; val details = "Tên: ${entry.name}\nĐường dẫn: ${entry.path}\nNgày sửa: ${DateFormatter.formatDateTime(entry.modified)}\nKích thước: $size\nMIME: ${entry.mime ?: "N/A"}"; AlertDialog.Builder(this).setTitle("Chi tiết file").setMessage(details).setPositiveButton("Copy đường dẫn") { _, _ -> copyPath(entry.path) }.setNegativeButton("Đóng", null).show() }
    private fun copyPath(path: String) { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Path", path)); Toast.makeText(this, "Đã copy đường dẫn", Toast.LENGTH_SHORT).show() }
    private fun openDiff(rootUri: String, displayName: String) { startActivity(Intent(this, DiffActivity::class.java).apply { putExtra(EXTRA_ROOT_URI, rootUri); putExtra(EXTRA_DISPLAY_NAME, displayName) }) }
    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.menu_tree, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_export_md -> { exportMarkdown(); true }
        R.id.action_export_json -> { exportJson(); true }
        R.id.action_copy_md -> { currentMarkdown()?.let { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Markdown", it)); Toast.makeText(this, "Đã copy Markdown", Toast.LENGTH_SHORT).show() }; true }
        R.id.action_share_md -> { currentMarkdown()?.let { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, it) }, "Chia sẻ Markdown")) }; true }
        R.id.action_scan_again -> { viewModel.rescan(); true }
        R.id.action_view_diff -> { viewModel.requestShowDiff(); true }
        else -> super.onOptionsItemSelected(item)
    }
    companion object { const val EXTRA_ROOT_URI = "extra_root_uri"; const val EXTRA_DISPLAY_NAME = "extra_display_name" }
}
