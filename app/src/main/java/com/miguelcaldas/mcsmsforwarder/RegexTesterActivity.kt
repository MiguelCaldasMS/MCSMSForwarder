package com.miguelcaldas.mcsmsforwarder

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.miguelcaldas.mcsmsforwarder.util.LogUtils
import com.miguelcaldas.mcsmsforwarder.util.RegexListStore
import com.miguelcaldas.mcsmsforwarder.util.SenderListStore
import com.miguelcaldas.mcsmsforwarder.util.TextNormalizer

class RegexTesterActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var rootContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_regex_tester)

        rootContainer = findViewById(R.id.rootContainer)
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

        val sampleSender = findViewById<EditText>(R.id.sampleSender)
        val sampleMessage = findViewById<EditText>(R.id.sampleMessage)
        val testPattern = findViewById<EditText>(R.id.testPattern)
        val testButton = findViewById<MaterialButton>(R.id.testButton)
        val savePatternButton = findViewById<MaterialButton>(R.id.savePatternButton)
        val testResult = findViewById<TextView>(R.id.testResult)

        sampleSender.setText(SenderListStore.load(prefs).firstOrNull() ?: "")
        testPattern.setText(RegexListStore.load(prefs).firstOrNull() ?: "")

        testButton.setOnClickListener {
            val sender = sampleSender.text.toString().trim()
            val allowedSenders = SenderListStore.load(prefs)
            val message = sampleMessage.text.toString()
            val patternText = testPattern.text.toString()

            if (patternText.isEmpty()) {
                testResult.text = "Please enter a regex pattern."
                return@setOnClickListener
            }

            val senderAllowed = allowedSenders.contains(sender)
            val regexMatch: MatchResult? = try {
                Regex(patternText).find(TextNormalizer.normalizeForMatching(message))
            } catch (e: Exception) {
                testResult.text = "⚠️ Invalid regex: ${e.message}"
                return@setOnClickListener
            }

            val matchText = regexMatch?.let { it.groups[1]?.value ?: it.value }
            val forwardTo = prefs.getString("forwardTo", null) ?: "(no destination set)"

            testResult.text = buildString {
                append("Sender allowed: $senderAllowed\n")
                append("Regex matched: ${regexMatch != null}\n")
                if (matchText != null) append("Would forward: \"$matchText\" to $forwardTo")
            }

            if (senderAllowed && matchText != null) {
                LogUtils.addToLog(this, "FAKE SEND → To: $forwardTo | Msg: $matchText")
                Snackbar.make(rootContainer, "Simulated send logged", Snackbar.LENGTH_SHORT).show()
            }
        }

        savePatternButton.setOnClickListener {
            val newPattern = testPattern.text.toString()
            if (newPattern.isEmpty()) {
                Snackbar.make(rootContainer, "Pattern is empty", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val current = RegexListStore.load(prefs)
            if (newPattern in current) {
                Snackbar.make(rootContainer, "Pattern already saved", Snackbar.LENGTH_SHORT).show()
            } else {
                RegexListStore.save(prefs, current + newPattern)
                Snackbar.make(rootContainer, "Pattern saved", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
