package com.example.fts.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.RootFolder
import com.example.fts.data.repository.SettingsRepository
import com.example.fts.data.storage.BroadStorageAccess
import com.example.fts.domain.export.JsonExporter
import com.example.fts.domain.export.MarkdownExporter
import com.example.fts.domain.scanner.Scanner
import com.example.fts.service.ScanService
import com.example.fts.ui.cache.CacheManageActivity
import com.example.fts.ui.settings.SettingsActivity
import com.example.fts.ui.tree.TreeActivity
import com.example.fts.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: RootFolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyRoots: TextView
    private val cacheManager by lazy { CacheManager(this) }
    private val settingsRepository by lazy { SettingsRepository(this) }
    private var pendingExport: String? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        val content = pendingExport
        pendingExport = null
        if (uri != null && content != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                        ?: error("Không thể mở tệp đích")
                }.onSuccess {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Đã xuất thành công", Toast.LENGTH_SHORT).show() }
                }.onFailure {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Xuất thất bại: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Folder Tree Snapshot"
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        viewModel = MainViewModel(application)
        recyclerView = findViewById(R.id.recyclerView)
        emptyRoots = findViewById(R.id.emptyRoots)
        adapter = RootFolderAdapter { root -> showRootActions(root) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        viewModel.rootFolders.observe(this) { folders ->
            adapter.submitList(folders)
            emptyRoots.visibility = if (folders.isEmpty()) TextView.VISIBLE else TextView.GONE
        }
        findViewById<android.widget.Button>(R.id.btn_select_folder).setOnClickListener { chooseRoot() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        R.id.action_cache_management -> { startActivity(Intent(this, CacheManageActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showRootActions(root: RootFolder) {
        val actions = arrayOf("Xem chi tiết", "Xuất Markdown", "Xuất JSON", "Quét lại", "Xóa")
        AlertDialog.Builder(this)
            .setTitle(root.displayName)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> openTree(root)
                    1 -> exportRoot(root, false)
                    2 -> exportRoot(root, true)
                    3 -> startScanService(root.uri, root.displayName, true)
                    4 -> confirmRemoveRoot(root)
                }
            }
            .show()
    }

    private fun openTree(root: RootFolder) {
        startActivity(Intent(this, TreeActivity::class.java).apply {
            putExtra(TreeActivity.EXTRA_ROOT_URI, root.uri)
            putExtra(TreeActivity.EXTRA_DISPLAY_NAME, root.displayName)
        })
    }

    private fun exportRoot(root: RootFolder, json: Boolean) {
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) { cacheManager.load(root.uri) }
            if (snapshot == null) {
                Toast.makeText(this@MainActivity, "Chưa có snapshot. Hãy quét thư mục trước.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val content = if (json) {
                JsonExporter.export(snapshot)
            } else {
                MarkdownExporter.export(snapshot, settingsRepository.markdownModel(), settingsRepository.showMetadataInMarkdown(), settingsRepository.sortOrder())
            }
            pendingExport = content
            createDocumentLauncher.launch("tree_${root.displayName}_${System.currentTimeMillis()}.${if (json) "json" else "md"}")
        }
    }

    private fun confirmRemoveRoot(root: RootFolder) {
        AlertDialog.Builder(this)
            .setTitle("Xóa khỏi danh sách?")
            .setMessage("Thư mục sẽ được bỏ khỏi màn hình chính. Snapshot/cache vẫn được giữ lại để có thể dùng khi thêm lại và quét lại sau này.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ -> viewModel.deleteRootFolder(root.uri) }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (BroadStorageAccess.hasAccess(this) && pendingPermissionRequest) {
            pendingPermissionRequest = false
            showFilesystemPicker(Environment.getExternalStorageDirectory())
        }
    }

    private var pendingPermissionRequest = false

    private fun chooseRoot() {
        if (!BroadStorageAccess.hasAccess(this)) {
            pendingPermissionRequest = true
            AlertDialog.Builder(this)
                .setTitle("Cấp quyền truy cập toàn bộ tệp")
                .setMessage("Ứng dụng cần quyền “Truy cập tất cả tệp” để duyệt và quét trực tiếp bằng API tệp của Android, nhanh hơn SAF. Quyền này có thể được cấp trong Cài đặt hệ thống.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Mở Cài đặt") { _, _ -> BroadStorageAccess.request(this) }
                .show()
            return
        }
        showFilesystemPicker(Environment.getExternalStorageDirectory())
    }

    private fun showFilesystemPicker(start: File) {
        var current = start
        fun render() {
            val children = current.listFiles()
                ?.asSequence()
                ?.filter { it.isDirectory && it.canRead() }
                ?.sortedBy { it.name.lowercase() }
                ?.toList()
                ?: emptyList()
            val labels = children.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Chọn thư mục\n${current.absolutePath}")
                .setItems(labels) { _, which -> current = children[which]; render() }
                .setNegativeButton(if (current.absolutePath == start.absolutePath) "Hủy" else "Thư mục cha") { _, _ ->
                    current.parentFile?.takeIf { it.canRead() }?.let { current = it; render() }
                }
                .setNeutralButton("Chọn thư mục này") { _, _ -> onFolderSelected(current) }
                .show()
        }
        render()
    }

    private fun onFolderSelected(folder: File) {
        if (!folder.isDirectory || !folder.canRead()) {
            Toast.makeText(this, "Không thể đọc thư mục này", Toast.LENGTH_LONG).show()
            return
        }
        val rootUri = Uri.fromFile(folder)
        val rootFolder = RootFolder(rootUri.toString(), folder.name.ifBlank { "Internal storage" }, System.currentTimeMillis())
        if (viewModel.rootFolders.value?.any { it.uri == rootFolder.uri } == true) {
            Toast.makeText(this, "Thư mục đã có trong danh sách", Toast.LENGTH_SHORT).show()
            return
        }
        if ((viewModel.rootFolders.value?.size ?: 0) >= Constants.MAX_FOLDERS) {
            Toast.makeText(this, "Tối đa ${Constants.MAX_FOLDERS} thư mục", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.saveRootFolder(rootFolder)
        lifecycleScope.launch {
            val estimate = withContext(Dispatchers.IO) { Scanner(this@MainActivity, CacheManager(this@MainActivity)).estimateCount(rootUri) }
            val estimateText = if (estimate >= 100000) "100.000+ mục; có thể mất vài phút" else "$estimate mục"
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Xác nhận quét")
                .setMessage("Thư mục: ${rootFolder.displayName}\nĐường dẫn: ${folder.absolutePath}\nƯớc tính: ~$estimateText\n\nQuét trực tiếp filesystem, không dùng SAF.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Bắt đầu quét") { _, _ -> startScanService(rootFolder.uri, rootFolder.displayName, false) }
                .show()
        }
    }

    private fun startScanService(rootUri: String, displayName: String, isIncremental: Boolean) {
        val intent = Intent(this, ScanService::class.java).apply {
            putExtra(Constants.EXTRA_URI, rootUri)
            putExtra(Constants.EXTRA_DISPLAY_NAME, displayName)
            putExtra(Constants.EXTRA_INCREMENTAL, isIncremental)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}
