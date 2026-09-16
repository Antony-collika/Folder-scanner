package com.example.fts.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.example.fts.data.saf.SafPermission
import com.example.fts.domain.scanner.Scanner
import com.example.fts.service.ScanService
import com.example.fts.ui.cache.CacheManageActivity
import com.example.fts.ui.settings.SettingsActivity
import com.example.fts.ui.tree.TreeActivity
import com.example.fts.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: RootFolderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyRoots: TextView
    private val selectFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(::onFolderSelected) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar)); supportActionBar?.title = "Folder Tree Snapshot"
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        viewModel = MainViewModel(application); recyclerView = findViewById(R.id.recyclerView); emptyRoots = findViewById(R.id.emptyRoots)
        adapter = RootFolderAdapter(
            onClick = { root -> startActivity(Intent(this, TreeActivity::class.java).apply { putExtra(TreeActivity.EXTRA_ROOT_URI, root.uri); putExtra(TreeActivity.EXTRA_DISPLAY_NAME, root.displayName) }) },
            onScanClick = { root -> startScanService(root.uri, root.displayName, true) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this); recyclerView.adapter = adapter
        viewModel.rootFolders.observe(this) { folders -> adapter.submitList(folders); emptyRoots.visibility = if (folders.isEmpty()) TextView.VISIBLE else TextView.GONE }
        findViewById<android.widget.Button>(R.id.btn_select_folder).setOnClickListener { selectFolderLauncher.launch(null) }
    }

    private fun onFolderSelected(uri: Uri) {
        val persisted = SafPermission.takePersistableUriPermission(this, uri)
        if (!persisted) Toast.makeText(this, "Không thể lưu quyền truy cập; bạn vẫn có thể quét lần này.", Toast.LENGTH_LONG).show()
        val rootFolder = RootFolder(uri.toString(), getDisplayName(uri), System.currentTimeMillis())
        if (viewModel.rootFolders.value?.any { it.uri == rootFolder.uri } == true) { Toast.makeText(this, "Thư mục đã có trong danh sách", Toast.LENGTH_SHORT).show(); return }
        if ((viewModel.rootFolders.value?.size ?: 0) >= Constants.MAX_FOLDERS) { Toast.makeText(this, "Tối đa ${Constants.MAX_FOLDERS} thư mục", Toast.LENGTH_SHORT).show(); return }
        viewModel.saveRootFolder(rootFolder)
        lifecycleScope.launch {
            val estimate = withContext(Dispatchers.IO) { Scanner(this@MainActivity, CacheManager(this@MainActivity)).estimateCount(uri) }
            val estimateText = if (estimate >= 100000) "~100.000+ mục; có thể mất vài phút" else "~$estimate mục"
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Xác nhận quét")
                .setMessage("Thư mục: ${rootFolder.displayName}\nƯớc tính: $estimateText\n\nỨng dụng sẽ quét nền và cập nhật cây khi hoàn tất.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Bắt đầu quét") { _, _ -> startScanService(rootFolder.uri, rootFolder.displayName, false) }
                .show()
        }
    }

    private fun getDisplayName(uri: Uri): String { val segments = (uri.path ?: "").split("/").filter { it.isNotEmpty() }; return segments.lastOrNull() ?: "Folder ${UUID.randomUUID().toString().take(8)}" }
    private fun startScanService(rootUri: String, displayName: String, isIncremental: Boolean) { val intent = Intent(this, ScanService::class.java).apply { putExtra(Constants.EXTRA_URI, rootUri); putExtra(Constants.EXTRA_DISPLAY_NAME, displayName); putExtra(Constants.EXTRA_INCREMENTAL, isIncremental) }; if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent) }
    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.menu_main, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) { R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }; R.id.action_cache_management -> { startActivity(Intent(this, CacheManageActivity::class.java)); true }; else -> super.onOptionsItemSelected(item) }
}
