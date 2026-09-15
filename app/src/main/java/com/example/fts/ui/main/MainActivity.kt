package com.example.fts.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fts.R
import com.example.fts.data.model.RootFolder
import com.example.fts.data.repository.RootFolderRepository
import com.example.fts.data.saf.SafPermission
import com.example.fts.databinding.ActivityMainBinding
import com.example.fts.ui.settings.SettingsActivity
import com.example.fts.ui.tree.TreeActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: RootFolderAdapter
    
    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFolderSelected(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        viewModel = MainViewModel(RootFolderRepository(this))
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
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
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = RootFolderAdapter { rootFolder ->
            openTreeActivity(rootFolder)
        }
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupObservers() {
        viewModel.rootFolders.observe(this) { folders ->
            adapter.submitList(folders)
            binding.emptyView.visibility = if (folders.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    private fun setupClickListeners() {
        binding.fabChooseFolder.setOnClickListener {
            openFolderPicker()
        }
    }
    
    private fun openFolderPicker() {
        val intent = Intent(DocumentsContract.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        pickFolderLauncher.launch(intent)
    }
    
    private fun handleFolderSelected(uri: Uri) {
        val hasPermission = SafPermission.takePersistableUriPermission(this, uri)
        
        val displayName = DocumentsContract.getTreeDocumentId(uri)
            .split(":").lastOrNull() ?: "Thu muc"
        
        val existing = viewModel.getFolderByUri(uri.toString())
        if (existing != null) {
            Toast.makeText(this, "Thu muc nay da co trong danh sach", Toast.LENGTH_SHORT).show()
            return
        }
        
        val rootFolder = RootFolder(
            uri = uri.toString(),
            displayName = displayName,
            addedAt = System.currentTimeMillis()
        )
        
        if (viewModel.addRootFolder(rootFolder)) {
            Toast.makeText(this, "Da them thu muc", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Khong the them thu muc", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openTreeActivity(rootFolder: RootFolder) {
        val intent = Intent(this, TreeActivity::class.java).apply {
            putExtra(TreeActivity.EXTRA_ROOT_URI, rootFolder.uri)
            putExtra(TreeActivity.EXTRA_ROOT_NAME, rootFolder.displayName)
        }
        startActivity(intent)
    }

}