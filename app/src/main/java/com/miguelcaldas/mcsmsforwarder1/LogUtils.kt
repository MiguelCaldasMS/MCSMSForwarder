package com.miguelcaldas.mcsmsforwarder1

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

private const val PREFS = "mc_sms_forwarder"
private const val LOGS_KEY = "logs"

fun addToLog(context: Context, logEntry: String) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val logs = prefs.getStringSet(LOGS_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    // Store as epochMillis: content
    logs.add("${System.currentTimeMillis()}: $logEntry")
    prefs.edit().putStringSet(LOGS_KEY, logs).apply()
}

fun getLogs(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val rawLogs = prefs.getStringSet(LOGS_KEY, emptySet()) ?: emptySet()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    // Format timestamp and sort by time
    return rawLogs.mapNotNull { entry ->
        val idx = entry.indexOf(":")
        if (idx <= 0) return@mapNotNull null
        val ts = entry.substring(0, idx).toLongOrNull() ?: return@mapNotNull null
        val rest = entry.substring(idx + 1)
        val formatted = dateFormat.format(Date(ts))
        Pair(ts, "$formatted →$rest")
    }.sortedBy { it.first }
     .map { it.second }
}

fun clearLogs(context: Context) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(LOGS_KEY, emptySet()).apply()
}
