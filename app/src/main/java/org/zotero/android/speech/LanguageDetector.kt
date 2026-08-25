package org.zotero.android.speech

import android.icu.util.ULocale
import android.os.Build
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LanguageDetector {

    private const val undeterminedLanguageTag = "und"

    fun canonicalVariation(baseLanguage: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null
        }
        val maximal = ULocale.addLikelySubtags(ULocale(baseLanguage))
        val region = maximal.country
        if (region.isNullOrEmpty()) {
            return null
        }
        return "$baseLanguage-$region"
    }

    val deviceLocale: String
        get() = Locale.getDefault().toLanguageTag()

    suspend fun detectLanguage(text: String): String {
        val identifier = LanguageIdentification.getClient()
        try {
            val baseLanguage = identifyLanguage(identifier, text) ?: "en"
            return resolveVariation(baseLanguage)
        } finally {
            identifier.close()
        }
    }

    suspend fun detectLanguage(texts: List<String>): String {
        val identifier = LanguageIdentification.getClient()
        try {
            val scores = mutableMapOf<String, Double>()
            for (text in texts) {
                val trimmed = text.trim()
                if (trimmed.isEmpty()) {
                    continue
                }
                val weight = trimmed.length.toDouble()
                val hypotheses = identifyPossibleLanguages(identifier, trimmed).take(3)
                if (hypotheses.isEmpty()) {
                    val dominant = identifyLanguage(identifier, trimmed)
                    if (dominant != null) {
                        scores[dominant] = (scores[dominant] ?: 0.0) + weight
                    }
                } else {
                    for (hypothesis in hypotheses) {
                        val language = hypothesis.languageTag
                        scores[language] =
                            (scores[language] ?: 0.0) + hypothesis.confidence * weight
                    }
                }
            }
            val baseLanguage = scores.maxByOrNull { it.value }?.key ?: return "en-US"
            return resolveVariation(baseLanguage)
        } finally {
            identifier.close()
        }
    }

    fun resolveVariation(baseLanguage: String): String {
        val availableVariations = Locale.getAvailableLocales()
            .map { it.toLanguageTag() }
            .filter { it.startsWith("$baseLanguage-") }
            .distinct()

        if (availableVariations.isEmpty()) {
            return "en-US"
        }

        if (availableVariations.size == 1) {
            return availableVariations[0]
        }

        val deviceLocale = deviceLocale
        val deviceBaseLanguage = deviceLocale.take(2)
        if (deviceBaseLanguage == baseLanguage && availableVariations.contains(deviceLocale)) {
            return deviceLocale
        }

        val canonicalVariation = canonicalVariation(baseLanguage)
        if (canonicalVariation != null && availableVariations.contains(canonicalVariation)) {
            return canonicalVariation
        }

        return availableVariations[0]
    }

    private suspend fun identifyLanguage(identifier: LanguageIdentifier, text: String): String? {
        val languageTag = suspendCancellableCoroutine { continuation ->
            identifier.identifyLanguage(text)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(undeterminedLanguageTag) }
        }
        return if (languageTag == undeterminedLanguageTag) null else languageTag
    }

    private suspend fun identifyPossibleLanguages(
        identifier: LanguageIdentifier,
        text: String
    ): List<IdentifiedLanguage> {
        return suspendCancellableCoroutine { continuation ->
            identifier.identifyPossibleLanguages(text)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(emptyList()) }
        }
    }
}