package com.miguelcaldas.mcsmsforwarder.util

import android.content.SharedPreferences

object RegexListStore {
    private const val KEY = "messageFormat"

    // Newline is the only separator: commas, spaces, etc. can appear inside regexes.
    // A legacy single-pattern value (stored before this feature) is returned as a
    // one-element list with no migration needed. Entries are not trimmed because
    // leading/trailing whitespace can be a deliberate part of a pattern.
    fun load(prefs: SharedPreferences): List<String> =
        prefs.getString(KEY, "")
            ?.split('\n')
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    fun save(prefs: SharedPreferences, patterns: List<String>) {
        val normalized = patterns.filter { it.isNotEmpty() }
        prefs.edit().putString(KEY, normalized.joinToString("\n")).apply()
    }
}
