package com.miguelcaldas.mcsmsforwarder.util

import java.text.Normalizer

object TextNormalizer {
    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    fun foldDiacritics(text: String): String =
        COMBINING_MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
}
