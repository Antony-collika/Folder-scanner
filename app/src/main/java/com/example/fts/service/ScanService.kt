package com.example.fts.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fts.R
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.Diff
import com.example.fts.data.repository.RootFolderRepository
import com.example.fts.data.repository.SettingsRepository
import com.example.fts.domain.diff.DiffEngine
import com.example.fts.domain.scanner.Scanner
import com.example.fts.ui.diff.DiffActivity
import com.example.fts.ui.tree.TreeActivity
import com.example.fts.util.Constants
import com.example.fts.util.Logging
import kotlinx.coroutines.*

class ScanService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var scanJob: Job? = null
    private var isCancelled = false
    private lateinit var scanner: Scanner; private lateinit var cacheManager: CacheManager
    private lateinit var rootFolderRepository: RootFolderRepository; private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() { super.onCreate(); cacheManager = CacheManager(this); scanner = Scanner(this, cacheManager); rootFolderRepository = RootFolderRepository(this); settingsRepository = SettingsRepository(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.ACTION_CANCEL_SCAN) { isCancelled = true; scanJob?.cancel(); stopSelfResult(startId); return START_NOT_STICKY }
        val rootUri = intent?.getStringExtra(Constants.EXTRA_URI); val rootName = intent?.getStringExtra(Constants.EXTRA_DISPLAY_NAME); val isIncremental = intent?.getBooleanExtra(Constants.EXTRA_INCREMENTAL, false) ?: false
        if (rootUri == null || rootName == null) { stopSelf(startId); return START_NOT_STICKY }
        isCancelled = false; startForeground(Constants.NOTIFICATION_ID_SCAN, createNotification("Đang chuẩn bị quét...")); scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                val previous = if (isIncremental) cacheManager.load(rootUri) else null
                val snapshot = scanner.scan(Uri.parse(rootUri), rootName, settingsRepository.scanOptions(), previous) { progress -> if (isActive && !isCancelled) updateNotification("Đang quét... ${progress.count} mục") }
                if (!isActive || isCancelled) throw CancellationException("Scan cancelled")
                val diff = if (previous != null) DiffEngine.compare(previous, snapshot) else null
                if (previous != null) cacheManager.savePrevious(rootUri, previous)
                cacheManager.save(rootUri, snapshot)
                rootFolderRepository.getByUri(rootUri)?.let { rootFolderRepository.update(it.copy(lastScannedAt = snapshot.scannedAt, entryCount = snapshot.stats.totalEntries)) }
                if (settingsRepository.notificationOnScanComplete()) {
                    val changes = diff?.let { it.added.size + it.removed.size + it.modified.size + it.renamed.size + it.moved.size }
                    showCompleteNotification(rootName, snapshot.stats.totalEntries, changes, rootUri, rootName, diff)
                }
            } catch (_: CancellationException) { showCancelledNotification(rootName) }
            catch (e: Exception) { Logging.e("Scan error: ${e.message}", e); showErrorNotification(rootName, e.message ?: "Unknown error") }
            finally { if (!isCancelled) stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scanJob?.cancel(); serviceScope.cancel(); super.onDestroy() }

    private fun createNotification(message: String): Notification {
        val openPending = PendingIntent.getActivity(this, 0, Intent(this, TreeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val cancelPending = PendingIntent.getService(this, Constants.NOTIFICATION_ID_SCAN, Intent(this, ScanService::class.java).apply { action = Constants.ACTION_CANCEL_SCAN }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID).setContentTitle("Folder Tree Snapshot").setContentText(message).setSmallIcon(R.drawable.ic_folder).setContentIntent(openPending).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hủy", cancelPending).setProgress(0, 0, true).setOngoing(true).build()
    }
    private fun updateNotification(message: String) { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(Constants.NOTIFICATION_ID_SCAN, createNotification(message)) }
    private fun showCompleteNotification(rootName: String, totalEntries: Int, changeCount: Int?, rootUri: String, displayName: String, diff: Diff?) {
        val message = if (changeCount != null && changeCount > 0) "Quét xong: $rootName: $totalEntries mục, $changeCount thay đổi" else "Quét xong: $rootName: $totalEntries mục"
        val intent = (if (changeCount != null && changeCount > 0 && diff != null) Intent(this, DiffActivity::class.java) else Intent(this, TreeActivity::class.java)).apply { putExtra(Constants.EXTRA_URI, rootUri); putExtra(Constants.EXTRA_DISPLAY_NAME, displayName); putExtra(TreeActivity.EXTRA_ROOT_URI, rootUri); putExtra(TreeActivity.EXTRA_DISPLAY_NAME, displayName) }
        val pending = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID).setContentTitle("Quét hoàn tất").setContentText(message).setSmallIcon(R.drawable.ic_folder).setContentIntent(pending).setAutoCancel(true).build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, notification)
    }
    private fun showCancelledNotification(rootName: String) { val n = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID).setContentTitle("Đã hủy quét").setContentText("Đã hủy quét $rootName").setSmallIcon(R.drawable.ic_folder).setAutoCancel(true).build(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, n) }
    private fun showErrorNotification(rootName: String, error: String) { val n = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID).setContentTitle("Lỗi quét").setContentText("Không thể quét $rootName: $error").setSmallIcon(R.drawable.ic_folder).setAutoCancel(true).build(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, n) }
}