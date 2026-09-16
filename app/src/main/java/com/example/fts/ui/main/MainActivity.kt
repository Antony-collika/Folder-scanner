package com.example.fts.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.RootFolder
import com.example.fts.data.repository.RootFolderRepository
import com.example.fts.data.saf.SafPermission
import com.example.fts.service.ScanService
import com.example.fts.ui.cache.CacheManageActivity
import com.example.fts.ui.settings.SettingsActivity
import com.example.fts.ui.tree.TreeActivity
import com.example.fts.util.Constants
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: RootFolderAdapter
    private lateinit var recyclerView: RecyclerView

    private val selectFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onFolderSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Folder Tree Snapshot"

        viewModel = MainViewModel(application)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = RootFolderAdapter(
            onClick = { rootFolder ->
                val intent = Intent(this, TreeActivity::class.java).apply {
                    putExtra(TreeActivity.EXTRA_ROOT_URI, rootFolder.uri)
                    putExtra(TreeActivity.EXTRA_DISPLAY_NAME, rootFolder.displayName)
                }
                startActivity(intent)
            },
            onScanClick = { rootFolder ->
                startScanService(rootFolder.uri, rootFolder.displayName, true)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        viewModel.rootFolders.observe(this) { rootFolders ->
            adapter.submitList(rootFolders)
        }

        findViewById<android.widget.Button>(R.id.btn_select_folder).setOnClickListener {
            selectFolderLauncher.launch(null)
        }
    }

    private fun onFolderSelected(uri: Uri) {
        val persisted = SafPermission.takePersistableUriPermission(this, uri)
        if (!persisted) {
            Toast.makeText(
                this,
                "Không thể lưu quyền truy cập. Thư mục chỉ dùng được trong phiên hiện tại.",
                Toast.LENGTH_LONG
            ).show()
        }

        val displayName = getDisplayName(uri)
        val rootFolder = RootFolder(
            uri = uri.toString(),
            displayName = displayName,
            addedAt = System.currentTimeMillis()
        )

        if (viewModel.rootFolders.value?.any { it.uri == rootFolder.uri } == true) {
            Toast.makeText(this, "Thư mục đã có trong danh sách", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.rootFolders.value?.size ?: 0 >= Constants.MAX_FOLDERS) {
            Toast.makeText(this, "Chỉ được tối đa ${Constants.MAX_FOLDERS} thư mục", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveRootFolder(rootFolder)
        startScanService(rootFolder.uri, rootFolder.displayName, false)
    }

    private fun getDisplayName(uri: Uri): String {
        val path = uri.path ?: return "Folder " + UUID.randomUUID().toString().take(8)
        val segments = path.split("/").filter { it.isNotEmpty() }
        return segments.lastOrNull() ?: "Folder"
    }

    private fun startScanService(rootUri: String, displayName: String, isIncremental: Boolean) {
        val intent = Intent(this, ScanService::class.java).apply {
            putExtra(Constants.EXTRA_URI, rootUri)
            putExtra(Constants.EXTRA_DISPLAY_NAME, displayName)
            putExtra(Constants.EXTRA_INCREMENTAL, isIncremental)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_cache_management -> {
                startActivity(Intent(this, CacheManageActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
