package com.example.fts.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.fts.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar)); supportActionBar?.setDisplayHomeAsUpEnabled(true); supportActionBar?.title = "Cài đặt"
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SettingsFragment()).commit()
    }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            findPreference<ListPreference>("markdown_model")?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                setOnPreferenceClickListener { showMarkdownModelPreview(); false }
            }
        }

        private fun showMarkdownModelPreview() {
            val previews = arrayOf(
                "A · Heading + danh sách phẳng\n# folder 1\n## folder 2\n- File 1\n- File 2\n## folder 3\n- File 3",
                "B · Heading + dải phân cách\n# folder 1\n---\n## folder 2\nFile 1\nFile 2\n---",
                "C · Danh sách lồng\n- folder 1/\n  - folder 2/\n    - File 1\n    - File 2",
                "D · Cây ASCII (mặc định)\nfolder 1/\n├── folder 2/\n│   ├── File 1\n│   └── File 2\n└── File 3"
            )
            AlertDialog.Builder(requireContext()).setTitle("4 mô hình Markdown").setMessage(previews.joinToString("\n\n")).setPositiveButton("Đóng", null).show()
        }
    }
}
