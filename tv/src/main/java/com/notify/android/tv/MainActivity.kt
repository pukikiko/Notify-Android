package com.notify.android.tv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.notify.android.tv.theme.NotifyTvTheme
import com.notify.android.tv.ui.TvRoot
import com.notify.core.ui.rootViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            NotifyTvTheme {
                TvRoot()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }
}
