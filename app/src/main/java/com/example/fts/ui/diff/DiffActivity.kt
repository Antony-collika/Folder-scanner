package com.example.fts.ui.diff

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
import com.example.fts.domain.export.DiffExporter
import com.example.fts.ui.tree.TreeActivity

class DiffActivity : AppCompatActivity() {

    private lateinit var viewModel: DiffViewModel
    private lateinit var adapter: DiffAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerFromTime: TextView
    private lateinit var headerToTime: TextView
    private lateinit var headerTotalChanges: TextView

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri ->
        uri?.let { viewModel.onExportFileCreated(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diff)

        recyclerView = findViewById(R.id.recyclerView)
        headerFromTime = findViewById(R.id.fromTime)
        headerToTime = findViewById(R.id.toTime)
        headerTotalChanges = findViewById(R.id.totalChanges)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val rootUri = intent.getStringExtra(TreeActivity.EXTRA_ROOT_URI)
        val displayName = intent.getStringExtra(TreeActivity.EXTRA_DISPLAY_NAME) ?: ""

        if (rootUri == null) {
            Toast.makeText(this, "Error: No folder selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = DiffViewModel(rootUri, displayName, application)

        adapter = DiffAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.diffItems.observe(this) { items ->
            adapter.submitList(items)
            updateHeader()
        }

        viewModel.exportResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Exported successfully", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(this, "Export failed: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadDiff()
    }

    private fun updateHeader() {
        val diff = viewModel.diff.value
        diff?.let {
            supportActionBar?.title = "Diff: " + viewModel.displayName
            headerFromTime.text = "From: " + com.example.fts.util.DateFormatter.formatDateTime(it.fromScannedAt)
            headerToTime.text = "To: " + com.example.fts.util.DateFormatter.formatDateTime(it.toScannedAt)
            headerTotalChanges.text = "Total changes: " + (it.added.size + it.removed.size + it.modified.size + it.renamed.size + it.moved.size).toString()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_diff, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_export_diff -> {
                val diff = viewModel.diff.value
                diff?.let {
                    val markdown = DiffExporter.exportMarkdown(it, viewModel.displayName)
                    val filename = "diff_" + viewModel.displayName + "_" + System.currentTimeMillis() + ".md"
                    createDocumentLauncher.launch(filename)
                    viewModel.prepareExport(markdown, "md")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}