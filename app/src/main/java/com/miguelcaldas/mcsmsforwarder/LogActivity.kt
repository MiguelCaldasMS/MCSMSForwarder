package com.miguelcaldas.mcsmsforwarder

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.miguelcaldas.mcsmsforwarder.util.LogUtils

class LogActivity : AppCompatActivity() {

    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val contentScroll = findViewById<View>(R.id.contentScroll)
        ViewCompat.setOnApplyWindowInsetsListener(contentScroll) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }

        logText = findViewById(R.id.logText)
        findViewById<MaterialButton>(R.id.clearLogs).setOnClickListener {
            LogUtils.clearLogs(this)
            refreshLogs()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
    }

    private fun refreshLogs() {
        val logs = LogUtils.getLogs(this)
        if (logs.isEmpty()) {
            logText.text = "No logs yet."
            return
        }
        val builder = SpannableStringBuilder()
        logs.forEach { entry ->
            val start = builder.length
            builder.append(entry).append("\n\n")
            val end = builder.length
            when {
                entry.contains("REAL SEND") -> builder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#2E7D32")),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                entry.contains("FAKE SEND") -> builder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#1565C0")),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        logText.text = builder
    }
}
