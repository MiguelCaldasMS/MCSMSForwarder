package com.miguelcaldas.mcsmsforwarder1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity: AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
    /* You can inspect results if needed */
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
            clearLogs(this)
            refreshLogs()
        }

        requestPermissions.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS
            )
        )

        refreshLogs()

        Toast.makeText(this, "SMS Forwarder started.", Toast.LENGTH_LONG).show()
    }

    private fun refreshLogs() {
        val logs = getLogs(this)
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
