package io.tolgee.formats.genericStructuredFile.`in`

import com.ibm.icu.util.IllformedLocaleException
import com.ibm.icu.util.ULocale
import io.tolgee.formats.importCommon.ImportFormat
import java.util.concurrent.atomic.AtomicLong

object FormatDetectionUtil {
  fun isValidBCP47Tag(tag: String): Boolean {
    return try {
      val locale = ULocale.forLanguageTag(tag)
      locale.toLanguageTag() == tag
    } catch (e: IllformedLocaleException) {
      false
    }
  }

  fun regexFactor(
    regex: Regex,
    weight: Double = 1.0,
  ): Factor {
    return Factor(weight) { it: Any? ->
      val hits = AtomicLong(0)
      val total = AtomicLong(0)
      processMapRecursive(it, regex, hits, total)
      hits.get().toDouble() / total.get().toDouble()
    }
  }

  private fun processMapRecursive(
    data: Any?,
    regex: Regex,
    hits: AtomicLong,
    total: AtomicLong,
  ) {
    when (data) {
      is Map<*, *> -> data.forEach { (_, value) -> processMapRecursive(value, regex, hits, total) }
      is List<*> -> data.forEach { item -> processMapRecursive(item, regex, hits, total) }
      is Array<*> -> data.forEach { item -> processMapRecursive(item, regex, hits, total) }
      else -> {
        if (data is String) {
          val count = regex.findAll(data).count()
          total.incrementAndGet()
          if (regex.containsMatchIn(data)) {
            hits.addAndGet(count.toLong())
          }
        }
      }
    }
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

  data class Factor(val weight: Double, val matcher: (Any?) -> Double)

  val ICU_DETECTION_REGEX =
    (
      "(?:^|\\s)" +
        "\\{" +
        "\\w+(\\s*,\\s*)?(((plural)\\s*,\\s*)?(.*other\\s*\\{.*\\}.*)|number,?.*)?" +
        "\\}" +
        "(?:\\W|\$)"
    )
      .toRegex()
}
