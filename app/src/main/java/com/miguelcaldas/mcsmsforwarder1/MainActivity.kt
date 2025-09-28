package com.miguelcaldas.mcsmsforwarder1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.miguelcaldas.mcsmsforwarder1.util.LogUtils

class MainActivity: AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.POST_NOTIFICATIONS,
//        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    )

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
            // This lambda is called when the user responds to the permission request dialog.
            // permissionsMap is a Map<String, Boolean> where String is the permission
            // and Boolean is true if granted, false otherwise.

            val allGranted = permissionsMap.all { it.value }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                // proceedWithAppFunctionality()
            } else {
                val deniedPermissions = permissionsMap.filter { !it.value }.keys

                val permanentlyDenied = deniedPermissions.any { permission -> !shouldShowRequestPermissionRationale(permission) }

                if (permanentlyDenied) {
                    Toast.makeText(this, "Some permissions were permanently denied. Please enable them in app settings.", Toast.LENGTH_LONG).show()
                } else {
                    // Some permissions were denied, but we can ask again.
                    // Show rationale for why these specific permissions are needed.
                    val permissionNames = deniedPermissions.joinToString { it.substringAfterLast(".") }
                    Toast.makeText(this, "The following permissions are needed: $permissionNames. The app might not work correctly without them.", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("mc_sms_forwarder", Context.MODE_PRIVATE)

        val destinationNumber = findViewById<EditText>(R.id.destinationNumber)
        val senderNumbers = findViewById<EditText>(R.id.senderNumbers)
        val messageFormat = findViewById<EditText>(R.id.messageFormat)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val openTester = findViewById<Button>(R.id.openTester)
        val clearLogsBtn = findViewById<Button>(R.id.clearLogs)
        logText = findViewById(R.id.logText)
        logScroll = findViewById(R.id.logScroll)

        destinationNumber.setText(prefs.getString("forwardTo", ""))
        senderNumbers.setText(prefs.getString("allowedSenders", ""))
        messageFormat.setText(prefs.getString("messageFormat", ""))

        saveButton.setOnClickListener {
            prefs.edit()
                .putString("forwardTo", destinationNumber.text.toString().trim())
                .putString("allowedSenders", senderNumbers.text.toString().trim())
                .putString("messageFormat", messageFormat.text.toString())
                .apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        openTester.setOnClickListener {
            startActivity(Intent(this, RegexTesterActivity::class.java))
        }

        clearLogsBtn.setOnClickListener {
            LogUtils.clearLogs(this)
            refreshLogs()
        }

        checkAndRequestPermissions()

        refreshLogs()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(REQUIRED_PERMISSIONS)
        }

        checkAndRequestBatteryOptimizationExemption()
    }

    private fun checkAndRequestBatteryOptimizationExemption() {
        val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(this.packageName)) {
            // Consider showing a dialog explaining why this is needed BEFORE sending the intent
            AlertDialog.Builder(this)
                .setTitle("Battery Optimization")
                .setMessage("To ensure reliable message forwarding in all conditions, this app can be exempted from battery optimizations. This may increase battery usage. Do you want to go to settings to make this change?")
                .setPositiveButton("Go to Settings") { _, _ ->
                    requestIgnoreBatteryOptimizations()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pn = this.packageName
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:${pn}")
        }
        if (intent.resolveActivity(this.packageManager) != null) {
            this.startActivity(intent)
        }
    }

    private fun refreshLogs() {
        val logs = LogUtils.getLogs(this)
        if (logs.isEmpty()) {
            logText.text = "No logs yet."
        } else {
            val builder = SpannableStringBuilder()
            logs.forEach { entry ->
                val start = builder.length
                builder.append(entry).append("\n\n")
                val end = builder.length

                when {
                    entry.contains("REAL SEND") -> builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#2E7D32")), // green
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    entry.contains("FAKE SEND") -> builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#1565C0")), // blue
                        start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            logText.text = builder

            logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
    }
}
