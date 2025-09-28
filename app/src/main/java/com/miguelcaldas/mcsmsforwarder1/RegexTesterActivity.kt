package com.miguelcaldas.mcsmsforwarder1

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.miguelcaldas.mcsmsforwarder1.util.LogUtils

class RegexTesterActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_regex_tester)

        prefs = getSharedPreferences("mc_sms_forwarder", Context.MODE_PRIVATE)

        val sampleSender = findViewById<EditText>(R.id.sampleSender)
        val sampleMessage = findViewById<EditText>(R.id.sampleMessage)
        val testPattern = findViewById<EditText>(R.id.testPattern)
        val testButton = findViewById<Button>(R.id.testButton)
        val savePatternButton = findViewById<Button>(R.id.savePatternButton)
        val testResult = findViewById<TextView>(R.id.testResult)

//        sampleSender.setText(prefs.getString("allowedSenders", "").split(",").firstOrNull()?.trim() ?: "")
        sampleSender.setText(
            prefs.getString("allowedSenders", null)
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?: ""
        )
        testPattern.setText(prefs.getString("messageFormat", ""))

        testButton.setOnClickListener {
            val sender = sampleSender.text.toString().trim()
            val allowedSenders = prefs.getString("allowedSenders", "")
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val message = sampleMessage.text.toString()
            val patternText = testPattern.text.toString()

            if (patternText.isEmpty()) {
                testResult.text = "Please enter a regex pattern."
                return@setOnClickListener
            }

            val senderAllowed = allowedSenders.contains(sender)
            val regexMatch: MatchResult? = try {
                Regex(patternText).find(message)
            } catch (e: Exception) {
                testResult.text = "⚠️ Invalid regex: ${e.message}"
                return@setOnClickListener
            }

//            val matchText = if (regexMatch != null) {
//                regexMatch.groups.getOrNull(1)?.value ?: regexMatch.value
//            } else null
            val matchText = if (regexMatch != null) {
                // Get the group by index. This will be null if the group doesn't exist.
                val group1 = regexMatch.groups[1]
                // Use the value of group 1 if it's not null, otherwise use the whole match value.
                group1?.value ?: regexMatch.value
            } else null

            val forwardTo = prefs.getString("forwardTo", null) ?: "(no destination set)"

            val resultSummary = buildString {
                append("Sender allowed: $senderAllowed\n")
                append("Regex matched: ${regexMatch != null}\n")
                if (matchText != null) append("Would forward: \"$matchText\" to $forwardTo")
            }

            testResult.text = resultSummary

            if (senderAllowed && matchText != null) {
                LogUtils.addToLog(this, "FAKE SEND → To: $forwardTo | Msg: $matchText")
                Toast.makeText(this, "Simulated send logged", Toast.LENGTH_SHORT).show()
            }
        }

        savePatternButton.setOnClickListener {
            val newPattern = testPattern.text.toString()
            prefs.edit().putString("messageFormat", newPattern).apply()
            Toast.makeText(this, "Pattern saved to settings", Toast.LENGTH_SHORT).show()
        }
    }
}
