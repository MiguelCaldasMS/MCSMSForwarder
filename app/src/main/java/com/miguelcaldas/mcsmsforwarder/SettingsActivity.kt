package com.miguelcaldas.mcsmsforwarder

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.miguelcaldas.mcsmsforwarder.util.RegexListStore
import com.miguelcaldas.mcsmsforwarder.util.SenderListStore

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var sendersContainer: LinearLayout
    private lateinit var regexesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

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

        prefs = getSharedPreferences("mc_sms_forwarder", Context.MODE_PRIVATE)

        val destinationNumber = findViewById<EditText>(R.id.destinationNumber)
        val openTester = findViewById<MaterialButton>(R.id.openTester)
        val addSenderButton = findViewById<MaterialButton>(R.id.addSenderButton)
        val addRegexButton = findViewById<MaterialButton>(R.id.addRegexButton)
        sendersContainer = findViewById(R.id.sendersContainer)
        regexesContainer = findViewById(R.id.regexesContainer)

        destinationNumber.setText(prefs.getString("forwardTo", ""))
        destinationNumber.addTextChangedListener { text ->
            prefs.edit { putString("forwardTo", text?.toString()?.trim().orEmpty()) }
        }

        SenderListStore.load(prefs).forEach { addSenderRow(it) }
        RegexListStore.load(prefs).forEach { addRegexRow(it) }

        addSenderButton.setOnClickListener {
            val row = addSenderRow("")
            row.findViewById<EditText>(R.id.senderEntry).requestFocus()
        }

        addRegexButton.setOnClickListener {
            val row = addRegexRow("")
            row.findViewById<EditText>(R.id.regexEntry).requestFocus()
        }

        openTester.setOnClickListener {
            startActivity(android.content.Intent(this, RegexTesterActivity::class.java))
        }
    }

    private fun addSenderRow(initialValue: String): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_sender, sendersContainer, false)
        val entry = row.findViewById<EditText>(R.id.senderEntry)
        val delete = row.findViewById<MaterialButton>(R.id.deleteSender)

        entry.setText(initialValue)
        entry.addTextChangedListener { persistSenders() }
        delete.setOnClickListener {
            sendersContainer.removeView(row)
            persistSenders()
        }

        sendersContainer.addView(row)
        return row
    }

    private fun persistSenders() {
        val list = (0 until sendersContainer.childCount).map { i ->
            sendersContainer.getChildAt(i)
                .findViewById<EditText>(R.id.senderEntry).text?.toString().orEmpty()
        }
        SenderListStore.save(prefs, list)
    }

    private fun addRegexRow(initialValue: String): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_regex, regexesContainer, false)
        val entry = row.findViewById<EditText>(R.id.regexEntry)
        val delete = row.findViewById<MaterialButton>(R.id.deleteRegex)

        entry.setText(initialValue)
        entry.addTextChangedListener { persistRegexes() }
        delete.setOnClickListener {
            regexesContainer.removeView(row)
            persistRegexes()
        }

        regexesContainer.addView(row)
        return row
    }

    private fun persistRegexes() {
        val list = (0 until regexesContainer.childCount).map { i ->
            regexesContainer.getChildAt(i)
                .findViewById<EditText>(R.id.regexEntry).text?.toString().orEmpty()
        }
        RegexListStore.save(prefs, list)
    }
}
