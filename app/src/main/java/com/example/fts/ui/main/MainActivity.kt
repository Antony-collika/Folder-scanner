package com.example.fts.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.RootFolder
import com.example.fts.data.storage.BroadStorageAccess
import com.example.fts.domain.scanner.Scanner
import com.example.fts.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar)); supportActionBar?.title = "Folder Tree Snapshot"
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        viewModel = MainViewModel(application)
        recyclerView = findViewById(R.id.recyclerView); emptyRoots = findViewById(R.id.emptyRoots)
        adapter = RootFolderAdapter(
            onClick = { root -> startActivity(Intent(this, TreeActivity::class.java).apply { putExtra(TreeActivity.EXTRA_ROOT_URI, root.uri); putExtra(TreeActivity.EXTRA_DISPLAY_NAME, root.displayName) }) },
            onScanClick = { root -> startScanService(root.uri, root.displayName, true) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this); recyclerView.adapter = adapter
        viewModel.rootFolders.observe(this) { folders -> adapter.submitList(folders); emptyRoots.visibility = if (folders.isEmpty()) TextView.VISIBLE else TextView.GONE }
        findViewById<android.widget.Button>(R.id.btn_select_folder).setOnClickListener { chooseRoot() }
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
            Toast.makeText(this, "Không thể đọc thư mục này", Toast.LENGTH_LONG).show(); return
        }
        val rootUri = Uri.fromFile(folder)
        val rootFolder = RootFolder(rootUri.toString(), folder.name.ifBlank { "Internal storage" }, System.currentTimeMillis())
        if (viewModel.rootFolders.value?.any { it.uri == rootFolder.uri } == true) { Toast.makeText(this, "Thư mục đã có trong danh sách", Toast.LENGTH_SHORT).show(); return }
        if ((viewModel.rootFolders.value?.size ?: 0) >= Constants.MAX_FOLDERS) { Toast.makeText(this, "Tối đa ${Constants.MAX_FOLDERS} thư mục", Toast.LENGTH_SHORT).show(); return }
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
        val intent = Intent(this, ScanService::class.java).apply { putExtra(Constants.EXTRA_URI, rootUri); putExtra(Constants.EXTRA_DISPLAY_NAME, displayName); putExtra(Constants.EXTRA_INCREMENTAL, isIncremental) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}
