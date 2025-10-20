package com.recuperavc.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object ExportNotification {

    private const val CHANNEL_ID = "recuperavc_exports"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Exportações"
            val desc = "Notificações de exportação de relatórios em PDF"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Shows a notification "PDF salvo" that opens the viewer when tapped.
     * pdfUri MUST be content:// (MediaStore or FileProvider).
     */
    fun notifyPdfSaved(context: Context, pdfUri: Uri, fileName: String) {
        if (!canPostNotifications(context)) return

        ensureChannel(context)

        // Intent to view the PDF with read grant
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("pdf", pdfUri)
        }

        val pendingFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

        val contentPending = PendingIntent.getActivity(
            context,
            0,
            viewIntent,
            pendingFlags
        )

        val title = "PDF salvo"
        val text = "Toque para abrir: $fileName"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // use your app icon; fully-qualified to avoid import issues
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Optional: secondary "Compartilhar" action
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("pdf", pdfUri)
        }
        val sharePending = PendingIntent.getActivity(
            context,
            1,
            Intent.createChooser(shareIntent, "Compartilhar PDF"),
            pendingFlags
        )
        builder.addAction(
            android.R.drawable.ic_menu_share,
            "Compartilhar",
            sharePending
        )

        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
