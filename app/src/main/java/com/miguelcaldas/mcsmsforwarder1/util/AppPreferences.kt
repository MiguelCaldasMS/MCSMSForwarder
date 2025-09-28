package com.miguelcaldas.mcsmsforwarder1.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.miguelcaldas.mcsmsforwarder1.data.LogEntry

object AppPreferences {
    private const val PREFS_NAME = "MCSMSForwarderPrefs"
    private const val KEY_TARGET_NUMBER = "target_number"
    private const val KEY_SENDER_FILTERS = "sender_filters"
    private const val KEY_REGEX_FILTERS = "regex_filters"
    private const val KEY_LOG_ENTRIES = "log_entries"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var targetNumber: String
        get() = prefs.getString(KEY_TARGET_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TARGET_NUMBER, value).apply()

    var senderFilters: List<String>
        get() = prefs.getString(KEY_SENDER_FILTERS, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_SENDER_FILTERS, value.joinToString("\n")).apply()

    var regexFilters: List<String>
        get() = prefs.getString(KEY_REGEX_FILTERS, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_REGEX_FILTERS, value.joinToString("\n")).apply()

    var logEntries: MutableList<LogEntry>
        get() {
            val json = prefs.getString(KEY_LOG_ENTRIES, null)
            return if (json != null) {
                val type = object : TypeToken<MutableList<LogEntry>>() {}.type
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_LOG_ENTRIES, json).apply()
        }

    fun addLogEntry(entry: LogEntry) {
        val logs = logEntries
        logs.add(0, entry) // Add to the top of the list
        logEntries = logs
    }

    fun clearLog() {
        logEntries = mutableListOf()
    }
}
