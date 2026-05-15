package com.miguelcaldas.mcsmsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.widget.Toast
import com.miguelcaldas.mcsmsforwarder.util.ForwardStatsStore
import com.miguelcaldas.mcsmsforwarder.util.LogUtils
import com.miguelcaldas.mcsmsforwarder.util.RegexListStore
import com.miguelcaldas.mcsmsforwarder.util.SenderListStore
import com.miguelcaldas.mcsmsforwarder.util.TextNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("mc_sms_forwarder", Context.MODE_PRIVATE)
        val allowedSenders = SenderListStore.load(prefs)
        val patterns = RegexListStore.load(prefs)
        val forwardTo = prefs.getString("forwardTo", "").orEmpty()
        val forwardTemplate = prefs.getString("forwardTemplate", "").orEmpty()

        // The telephony framework reassembles concatenated SMS using the UDH (reference,
        // total parts, sequence number) and only broadcasts SMS_RECEIVED once every part
        // has arrived. The returned array therefore represents a single logical message
        // with its segments already ordered; concatenating their bodies yields the full text.
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val fullBody = buildString {
            for (sms in messages) append(sms.messageBody ?: "")
        }

        // Match each configured entry either as an exact (case-insensitive) string
        // — so alphanumeric sender IDs like "AMAZON" still work — or via
        // PhoneNumberUtils.areSamePhoneNumber, which ignores formatting (+, spaces,
        // parens, dashes) and tolerates country-code prefix variations.
        val countryIso = deviceCountryIso(context)
        val senderAllowed = allowedSenders.any { entry ->
            entry.equals(sender, ignoreCase = true) ||
                PhoneNumberUtils.areSamePhoneNumber(entry, sender, countryIso)
        }
        if (!senderAllowed) return
        // Forward if ANY configured regex matches. Invalid regex syntax is silently
        // treated as a non-match — a single malformed entry never blocks the others.
        // Diacritics are stripped from the body before matching so patterns can be
        // written without accents; the original (accented) body is still forwarded.
        val normalizedBody = TextNormalizer.foldDiacritics(fullBody)
        val bodyMatches = patterns.any { pat ->
            try { Regex(pat).containsMatchIn(normalizedBody) } catch (_: Exception) { false }
        }
        if (!bodyMatches) return
        if (forwardTo.isEmpty()) return

        val outgoingBody = if (forwardTemplate.isEmpty()) fullBody
            else applyTemplate(forwardTemplate, sender, messages[0].timestampMillis, fullBody)

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(outgoingBody)
            smsManager.sendMultipartTextMessage(forwardTo, null, parts, null, null)
            ForwardStatsStore.recordForward(context)
            LogUtils.addToLog(context, "REAL SEND → To: $forwardTo | Msg: $outgoingBody")
        } catch (e: Exception) {
            LogUtils.addToLog(context, "REAL SEND FAILED → To: $forwardTo | Error: ${e.message}")
            Toast.makeText(context, "Forward failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Single-pass substitution of %s/%t/%m so tokens inside `message` are not re-expanded
    // and a literal `%` followed by any other character is left untouched.
    private fun applyTemplate(template: String, source: String, timestampMillis: Long, message: String): String {
        val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date(timestampMillis))
        val out = StringBuilder(template.length + message.length)
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c == '%' && i + 1 < template.length) {
                when (template[i + 1]) {
                    's' -> { out.append(source); i += 2; continue }
                    't' -> { out.append(time); i += 2; continue }
                    'm' -> { out.append(message); i += 2; continue }
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    private fun deviceCountryIso(context: Context): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val iso = tm?.networkCountryIso?.takeIf { it.isNotEmpty() }
            ?: tm?.simCountryIso?.takeIf { it.isNotEmpty() }
            ?: Locale.getDefault().country
        return iso.lowercase(Locale.ROOT)
    }
}
