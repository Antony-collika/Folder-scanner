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
import com.example.fts.data.model.Snapshot
import com.example.fts.data.model.ScanOptions
import com.example.fts.data.repository.RootFolderRepository
import com.example.fts.domain.diff.DiffEngine
import com.example.fts.domain.scanner.Scanner
import com.example.fts.domain.scanner.ScanProgress
import com.example.fts.ui.tree.TreeActivity
import com.example.fts.util.Constants
import com.example.fts.util.Logging
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
        scanner = Scanner(this, CacheManager(this))
        cacheManager = CacheManager(this)
        rootFolderRepository = RootFolderRepository(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rootUri = intent?.getStringExtra(Constants.SERVICE_EXTRA_URI)
        val rootName = intent?.getStringExtra(Constants.SERVICE_EXTRA_ROOT_NAME)
        val isIncremental = intent?.getBooleanExtra(Constants.SERVICE_EXTRA_IS_INCREMENTAL, false) ?: false
        
        if (rootUri == null || rootName == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForeground(createNotification("Starting scan...", 0))
        
        scanJob = serviceScope.launch {
            try {
                val options = ScanOptions()
                val uri = Uri.parse(rootUri)
                
                val snapshot = scanner.scan(uri, rootName, options) { progress ->
                    if (isActive && !isCancelled) {
                        updateNotification("Scanning...", progress.count)
                    }
                }
                
                cacheManager.save(rootUri, snapshot)
                
                val rootFolder = rootFolderRepository.getByUri(rootUri)
                if (rootFolder != null) {
                    rootFolderRepository.update(rootFolder.copy(
                        lastScannedAt = snapshot.scannedAt,
                        entryCount = snapshot.stats.totalEntries
                    ))
                }
                
                var diff: Diff? = null
                if (isIncremental) {
                    val oldSnapshot = cacheManager.load(rootUri)
                    if (oldSnapshot != null) {
                        diff = DiffEngine.compare(oldSnapshot, snapshot)
                    }
                }
                
                showCompleteNotification(rootName, snapshot.stats.totalEntries, diff?.totalChanges)
                
            } catch (e: Exception) {
                Logging.e("Scan error: ${e.message}", e)
                showErrorNotification(rootName, e.message ?: "Unknown error")
            } finally {
                if (!isCancelled) {
                    stopForeground(true)
                }
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
    
    private fun createNotification(message: String, count: Int): Notification {
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
            .addAction(R.drawable.ic_error, "Cancel") {
                isCancelled = true
                scanJob?.cancel()
                stopForeground(true)
                stopSelf()
            }
            .build()
    }
    
    private fun updateNotification(message: String, count: Int) {
        val notification = createNotification(message, count)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID_SCAN, notification)
    }
    
    private fun showCompleteNotification(rootName: String, totalEntries: Int, changeCount: Int?) {
        val message = if (changeCount != null && changeCount > 0) {
            "Scan complete: $rootName: $totalEntries entries, $changeCount changes"
        } else {
            "Scan complete: $rootName: $totalEntries entries"
        }
        
        val intent = Intent(this, TreeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Scan Complete")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_check)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, notification)
    }
    
    private fun showErrorNotification(rootName: String, error: String) {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Scan Error")
            .setContentText("Failed to scan $rootName: $error")
            .setSmallIcon(R.drawable.ic_error)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID_SCAN_COMPLETE, notification)
    }
}