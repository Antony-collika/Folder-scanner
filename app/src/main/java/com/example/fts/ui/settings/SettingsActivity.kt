package com.example.fts.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.fts.R
import com.example.fts.data.storage.BroadStorageAccess
import com.example.fts.ui.cache.CacheManageActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cài đặt"
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SettingsFragment()).commit()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var storagePreference: SwitchPreferenceCompat? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            findPreference<ListPreference>("markdown_model")?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                setOnPreferenceClickListener { showMarkdownModelPreview(); false }
            }
            storagePreference = findPreference("storage_access")
            storagePreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) BroadStorageAccess.request(requireActivity()) else showStorageDisableInfo()
                false
            }
            findPreference<Preference>("cache_management")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), CacheManageActivity::class.java))
                true
            }
        }

        override fun onResume() {
            super.onResume()
            val granted = BroadStorageAccess.hasAccess(requireActivity())
            storagePreference?.isChecked = granted
            storagePreference?.summary = if (granted) "Đã cấp quyền Storage · quét filesystem nhanh hơn" else "Chưa cấp quyền · ứng dụng sẽ dùng SAF"
        }

        private fun showStorageDisableInfo() {
            AlertDialog.Builder(requireContext()).setTitle("Quyền Storage")
                .setMessage("Quyền này không bắt buộc. Khi không có quyền Storage, ứng dụng vẫn hoạt động bằng SAF.")
                .setPositiveButton("Đóng", null).show()
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
