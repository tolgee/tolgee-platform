package io.tolgee.formats.genericStructuredFile.`in`

import com.ibm.icu.util.IllformedLocaleException
import com.ibm.icu.util.ULocale
import io.tolgee.formats.importCommon.ImportFormat

object FormatDetectionUtil {
  fun isValidBCP47Tag(tag: String): Boolean {
    return try {
      val locale = ULocale.forLanguageTag(tag)
      locale.toLanguageTag() == tag
    } catch (e: IllformedLocaleException) {
      false
    }
  }

  /** Per-factor sampling cap — the hit-rate ratio converges well before this. */
  private const val MAX_STRINGS_TO_INSPECT = 1000L

  fun regexFactor(
    regex: Regex,
    weight: Double = 1.0,
  ): Factor {
    return Factor(weight) { it: Any? ->
      // counters[0] = hits, counters[1] = total
      val counters = LongArray(2)
      processMapRecursive(it, regex, counters)
      val total = counters[1]
      if (total == 0L) 0.0 else counters[0].toDouble() / total.toDouble()
    }
  }

  /** @return false once the inspection cap is reached so the caller can short-circuit. */
  private fun processMapRecursive(
    data: Any?,
    regex: Regex,
    counters: LongArray,
  ): Boolean {
    if (counters[1] >= MAX_STRINGS_TO_INSPECT) return false
    when (data) {
      is Map<*, *> ->
        for ((_, value) in data) {
          if (!processMapRecursive(value, regex, counters)) return false
        }
      is List<*> ->
        for (item in data) {
          if (!processMapRecursive(item, regex, counters)) return false
        }
      is Array<*> ->
        for (item in data) {
          if (!processMapRecursive(item, regex, counters)) return false
        }
      is String -> {
        counters[1]++
        counters[0] += regex.findAll(data).count().toLong()
      }
      else -> Unit
    }
    return true
  }

  fun detectFromPossibleFormats(
    possibleFormats: Map<ImportFormat, Array<Factor>>,
    data: Any?,
  ): ImportFormat? {
    val scores =
      possibleFormats.map { (format, factors) ->
        val score = factors.sumOf { it.matcher(data) * it.weight }
        format to score
      }

    return scores.filter { it.second != 0.0 }.maxByOrNull { it.second }?.first
  }

  data class Factor(
    val weight: Double,
    val matcher: (Any?) -> Double,
  )

  val ICU_DETECTION_REGEX =
    (
      "(?:^|\\s)" +
        "\\{" +
        "\\w+(\\s*,\\s*)?(((plural)\\s*,\\s*)?(.*other\\s*\\{.*\\}.*)|number,?.*)?" +
        "\\}" +
        "(?:\\W|\$)"
    ).toRegex()
}
