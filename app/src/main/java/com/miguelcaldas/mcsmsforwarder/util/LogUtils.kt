package com.miguelcaldas.mcsmsforwarder.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LogUtils {
    private const val PREFS = "mc_sms_forwarder"
    private const val LOGS_KEY = "logs_v2"
    private const val LEGACY_LOGS_KEY = "logs"
    private const val FIELD_SEP = "\u001F"
    private const val LINE_SEP = "\n"
    private val RETENTION_MS = TimeUnit.DAYS.toMillis(35)
    private const val MAX_ENTRIES = 2000

    private data class Entry(val timestamp: Long, val message: String)

    fun addToLog(context: Context, logEntry: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entries = loadEntries(prefs).toMutableList()
        entries.add(Entry(System.currentTimeMillis(), logEntry))
        val pruned = prune(entries)
        prefs.edit { putString(LOGS_KEY, serialize(pruned)) }
    }

    /** Most recent first, formatted for display. */
    fun getLogs(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entries = loadEntries(prefs)
        val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        return entries
            .sortedByDescending { it.timestamp }
            .map { "${fmt.format(Date(it.timestamp))} → ${it.message}" }
    }

    fun clearLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            remove(LOGS_KEY)
            remove(LEGACY_LOGS_KEY)
        }
    }

    private fun loadEntries(prefs: SharedPreferences): List<Entry> {
        val raw = prefs.getString(LOGS_KEY, null)
        if (raw != null) return parse(raw)

        val legacy = runCatching { prefs.getStringSet(LEGACY_LOGS_KEY, emptySet()) }
            .getOrNull()
            ?: emptySet()
        if (legacy.isEmpty()) return emptyList()

        val migrated = legacy.mapNotNull { line ->
            val idx = line.indexOf(":")
            if (idx <= 0) return@mapNotNull null
            val ts = line.substring(0, idx).toLongOrNull() ?: return@mapNotNull null
            Entry(ts, line.substring(idx + 1).trimStart())
        }
        val pruned = prune(migrated.toMutableList())
        prefs.edit {
            putString(LOGS_KEY, serialize(pruned))
            remove(LEGACY_LOGS_KEY)
        }
        return pruned
    }

    private fun prune(entries: MutableList<Entry>): List<Entry> {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        entries.removeAll { it.timestamp < cutoff }
        if (entries.size > MAX_ENTRIES) {
            entries.sortBy { it.timestamp }
            repeat(entries.size - MAX_ENTRIES) { entries.removeAt(0) }
        }
        return entries
    }

    private fun serialize(entries: List<Entry>): String =
        entries.joinToString(LINE_SEP) { "${it.timestamp}$FIELD_SEP${it.message}" }

    private fun parse(raw: String): List<Entry> {
        if (raw.isEmpty()) return emptyList()
        return raw.split(LINE_SEP).mapNotNull { line ->
            val idx = line.indexOf(FIELD_SEP)
            if (idx <= 0) return@mapNotNull null
            val ts = line.substring(0, idx).toLongOrNull() ?: return@mapNotNull null
            Entry(ts, line.substring(idx + 1))
        }
    }
}