package com.miguelcaldas.mcsmsforwarder1

import android.content.Context
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.content.ContextCompat // Import ContextCompat
import com.miguelcaldas.mcsmsforwarder1.util.LogUtils

class SmsForwardWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val message = inputData.getString("message") ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("mc_sms_forwarder", Context.MODE_PRIVATE)
        val forwardTo = prefs.getString("forwardTo", null) ?: return Result.failure()

        return try {
            // Get SmsManager using getSystemService
            val smsManager = ContextCompat.getSystemService(applicationContext, SmsManager::class.java)
            // It's good practice to check if smsManager is null,
            // though for SmsManager it's generally available.
            if (smsManager != null) {
                smsManager.sendTextMessage(forwardTo, null, message, null, null)
                LogUtils.addToLog(applicationContext, "REAL SEND → To: $forwardTo | Msg: $message")
                Result.success()
            } else {
                LogUtils.addToLog(applicationContext, "REAL SEND FAILED → SmsManager not available")
                Result.failure()
            }
        } catch (e: Exception) {
            LogUtils.addToLog(applicationContext, "REAL SEND FAILED → To: $forwardTo | Error: ${e.message}")
            Result.failure()
        }
    }
}
