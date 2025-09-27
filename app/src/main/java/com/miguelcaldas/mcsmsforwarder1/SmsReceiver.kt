package com.miguelcaldas.mcsmsforwarder1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("mc_sms_forwarder", Context.MODE_PRIVATE)
        val allowedSenders = prefs.getString("allowedSenders", "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val patternString = prefs.getString("messageFormat", "").orEmpty()
        val formatPattern = if (patternString.isNotEmpty()) Regex(patternString) else null

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: continue
            val body = sms.messageBody ?: continue

            // Display a Toast for every received SMS
            val toastMessage = "SMS from: $sender\n$body"
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()

            if (allowedSenders.contains(sender) && formatPattern?.containsMatchIn(body) == true) {
                val match = formatPattern.find(body)
//                val contentToForward = match?.groups?.getOrNull(1)?.value ?: match?.value ?: body
                val contentToForward = match?.groups?.get(1)?.value ?: match?.value ?: body

                val data = Data.Builder()
                    .putString("message", contentToForward)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<SmsForwardWorker>()
                    .setInputData(data)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
