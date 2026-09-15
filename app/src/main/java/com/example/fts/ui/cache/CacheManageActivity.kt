package com.example.fts.ui.cache

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fts.R
import com.example.fts.data.model.RootFolder

class CacheManageActivity : AppCompatActivity() {

    private lateinit var viewModel: CacheManageViewModel
    private lateinit var adapter: CacheManageAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cache_manage)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = CacheManageViewModel(application)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = CacheManageAdapter(
            onDeleteClick = { rootFolder ->
                viewModel.deleteCache(rootFolder)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.cacheItems.observe(this) { items ->
            adapter.submitList(items)
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Cache deleted", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(this, "Delete failed: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadCacheItems()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cache_manage, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete_all -> {
                viewModel.deleteAllCache()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}