package com.miguelcaldas.mcsmsforwarder.util

import android.content.SharedPreferences

object SenderListStore {
    private const val KEY = "allowedSenders"

    // Split on newline and comma so values stored by previous app versions
    // (comma-separated) keep working until the user edits and re-saves.
    private val SEPARATORS = charArrayOf('\n', ',')

    fun load(prefs: SharedPreferences): List<String> =
        prefs.getString(KEY, "")
            ?.split(*SEPARATORS)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    fun save(prefs: SharedPreferences, senders: List<String>) {
        val normalized = senders.map { it.trim() }.filter { it.isNotEmpty() }
        prefs.edit().putString(KEY, normalized.joinToString("\n")).apply()
    }
}
