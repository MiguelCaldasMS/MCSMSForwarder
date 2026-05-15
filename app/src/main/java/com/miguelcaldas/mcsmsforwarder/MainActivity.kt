package com.miguelcaldas.mcsmsforwarder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.miguelcaldas.mcsmsforwarder.util.ForwardStatsStore
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: View
    private lateinit var statCount: TextView
    private lateinit var statFirst: TextView
    private lateinit var statLast: TextView

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
            val denied = permissionsMap.filterValues { !it }.keys
            if (denied.isEmpty()) return@registerForActivityResult

            val permanentlyDenied = denied.any { !shouldShowRequestPermissionRationale(it) }
            if (permanentlyDenied) {
                Snackbar.make(
                    rootContainer,
                    "Some permissions were permanently denied. Enable them in app settings.",
                    Snackbar.LENGTH_LONG
                ).setAction("Settings") {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                }.show()
            } else {
                val names = denied.joinToString { it.substringAfterLast(".") }
                Snackbar.make(rootContainer, "Needed permissions: $names", Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootContainer = findViewById(R.id.rootContainer)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        val contentScroll = findViewById<View>(R.id.contentScroll)
        ViewCompat.setOnApplyWindowInsetsListener(contentScroll) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }

        statCount = findViewById(R.id.statCount)
        statFirst = findViewById(R.id.statFirst)
        statLast = findViewById(R.id.statLast)

        findViewById<MaterialButton>(R.id.openSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.openLog).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val stats = ForwardStatsStore.load(this)
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        statCount.text = stats.count.toString()
        statFirst.text = if (stats.hasAny) fmt.format(Date(stats.firstMillis)) else "—"
        statLast.text = if (stats.hasAny) fmt.format(Date(stats.lastMillis)) else "—"
    }

    private fun checkAndRequestPermissions() {
        val toRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }
        checkAndRequestBatteryOptimizationExemption()
    }

    private fun checkAndRequestBatteryOptimizationExemption() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Battery optimization")
            .setMessage(
                "Exempt this app from battery optimization for reliable forwarding? " +
                    "This may increase battery usage."
            )
            .setPositiveButton("Settings") { _, _ -> requestIgnoreBatteryOptimizations() }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    }
}
