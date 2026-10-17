package com.example.fts.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.fts.R
import com.example.fts.data.repository.SettingsRepository
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
        private val settings by lazy { SettingsRepository(requireContext()) }

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
            findPreference<Preference>("content_root_limit")?.setOnPreferenceClickListener {
                showSizeLimitDialog(rootLimit = true); true
            }
            findPreference<Preference>("content_file_limit")?.setOnPreferenceClickListener {
                showSizeLimitDialog(rootLimit = false); true
            }
            updateLimitSummaries()
        }

        override fun onResume() {
            super.onResume()
            val granted = BroadStorageAccess.hasAccess(requireActivity())
            storagePreference?.isChecked = granted
            storagePreference?.summary = if (granted) "Đã cấp quyền Storage · quét filesystem nhanh hơn" else "Chưa cấp quyền · ứng dụng sẽ dùng SAF"
            updateLimitSummaries()
        }

        private fun updateLimitSummaries() {
            findPreference<Preference>("content_root_limit")?.summary =
                "Tối đa ${settings.contentRootLimitValue()} ${settings.contentRootLimitUnit()} dữ liệu tệp (1 KB = 1.024 byte)"
            findPreference<Preference>("content_file_limit")?.summary =
                "Tối đa ${settings.contentPerFileLimitValue()} ${settings.contentPerFileLimitUnit()} cho mỗi tệp"
        }

        private fun showSizeLimitDialog(rootLimit: Boolean) {
            val value = EditText(requireContext()).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setSingleLine(true)
                hint = "Nhập số nguyên dương"
                setText((if (rootLimit) settings.contentRootLimitValue() else settings.contentPerFileLimitValue()).toString())
                selectAll()
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val unit = Spinner(requireContext()).apply {
                adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, SettingsRepository.UNITS)
                val current = if (rootLimit) settings.contentRootLimitUnit() else settings.contentPerFileLimitUnit()
                setSelection(SettingsRepository.UNITS.indexOf(current).coerceAtLeast(0))
                layoutParams = LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(24), dp(8), dp(24), dp(4))
                addView(value)
                addView(unit)
            }
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(if (rootLimit) "Tổng dung lượng thư mục gốc" else "Dung lượng tối đa mỗi tệp")
                .setMessage("Nhập số nguyên dương. 1 KB = 1.024 byte; 1 MB = 1.024 KB.")
                .setView(row)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val parsed = value.text.toString().trim().toIntOrNull()
                    if (parsed == null || parsed <= 0) {
                        value.error = "Hãy nhập số nguyên lớn hơn 0"
                        return@setOnClickListener
                    }
                    try {
                        if (rootLimit) settings.setContentRootLimit(parsed, unit.selectedItem.toString())
                        else settings.setContentPerFileLimit(parsed, unit.selectedItem.toString())
                        updateLimitSummaries()
                        dialog.dismiss()
                    } catch (_: IllegalArgumentException) {
                        value.error = "Giá trị không hợp lệ"
                    }
                }
            }
            dialog.show()
        }

        private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
