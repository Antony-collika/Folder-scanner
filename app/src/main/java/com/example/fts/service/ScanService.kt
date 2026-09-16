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
import com.example.fts.data.model.ScanOptions
import com.example.fts.data.repository.RootFolderRepository
import com.example.fts.domain.diff.DiffEngine
import com.example.fts.domain.scanner.Scanner
import com.example.fts.util.Constants
import com.example.fts.util.Logging
import com.example.fts.ui.diff.DiffActivity
import com.example.fts.ui.tree.TreeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScanService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var scanJob: Job? = null
    private var isCancelled = false
    private lateinit var scanner: Scanner
    private lateinit var cacheManager: CacheManager
    private lateinit var rootFolderRepository: RootFolderRepository

    override fun onCreate() {
        super.onCreate()
        cacheManager = CacheManager(this)
        scanner = Scanner(this, cacheManager)
        rootFolderRepository = RootFolderRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rootUri = intent?.getStringExtra(Constants.EXTRA_URI)
        val rootName = intent?.getStringExtra(Constants.EXTRA_DISPLAY_NAME)
        val isIncremental = intent?.getBooleanExtra(Constants.EXTRA_INCREMENTAL, false) ?: false
        
        if (rootUri == null || rootName == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(Constants.NOTIFICATION_ID_SCAN, createNotification("Starting scan..."))
        scanJob = serviceScope.launch {
            try {
                val options = ScanOptions()
                val uri = Uri.parse(rootUri)
                val previousSnapshot = if (isIncremental) cacheManager.load(rootUri) else null
                
                val snapshot = scanner.scan(uri, rootName, options) { progress ->
                    if (isActive && !isCancelled) updateNotification("Scanning... ${progress.count}")
                }
                
                cacheManager.save(rootUri, snapshot)

                rootFolderRepository.getByUri(rootUri)?.let { rootFolderRepository.update(it.copy(
                    lastScannedAt = snapshot.scannedAt,
                    entryCount = snapshot.stats.totalEntries
                )) }

                // FIX: Calculate diff if incremental
                val diff = if (isIncremental && previousSnapshot != null) {
                    DiffEngine.compare(previousSnapshot, snapshot)
                } else null
                
                val changeCount = diff?.let { 
                    it.added.size + it.removed.size + it.modified.size + it.renamed.size + it.moved.size 
                }
                
                // FIX: Pass diff to notification
                showCompleteNotification(rootName, snapshot.stats.totalEntries, changeCount, rootUri, displayName, diff)
                
            } catch (e: Exception) {
                Logging.e("Scan error: ${e.message}", e)
                showErrorNotification(rootName, e.message ?: "Unknown error")
            } finally {
                if (!isCancelled) stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        serviceScope.cancel()
    }

    private fun createNotification(message: String): Notification {
        val intent = Intent(this, TreeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Folder Tree Snapshot")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_folder)
            .setContentIntent(pendingIntent)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID_SCAN, createNotification(message))
    }

    // FIX: Add diff parameter
    private fun showCompleteNotification(rootName: String, totalEntries: Int, changeCount: Int?, rootUri: String, displayName: String, diff: com.example.fts.data.model.Diff?) {
        val message = if (changeCount != null && changeCount > 0) {
            "Quét xong: $rootName: $totalEntries mục, $changeCount thay đổi"
        } else {
            "Quét xong: $rootName: $totalEntries mục"
        }
        
        val intent = if (changeCount != null && changeCount > 0 && diff != null) {
            // FIX: Open DiffActivity if there are changes
            Intent(this, DiffActivity::class.java).apply {
                putExtra(Constants.EXTRA_URI, rootUri)
                putExtra(Constants.EXTRA_DISPLAY_NAME, displayName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(this, TreeActivity::class.java).apply {
                putExtra(Constants.EXTRA_URI, rootUri)
                putExtra(Constants.EXTRA_DISPLAY_NAME, displayName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Quét hoàn tất")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_folder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, notification)
    }

    private fun showErrorNotification(rootName: String, error: String) {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Lỗi quét")
            .setContentText("Không thể quét $rootName: $error")
            .setSmallIcon(R.drawable.ic_folder)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, notification)
    }
}