package io.tolgee.service.aiMatchStats

import kotlin.math.roundToInt

/**
 * Word-level similarity between the AI output and the reviewed text, as an integer 0-100.
 *
 * 100 is reserved for an exact token match: any non-zero edit distance is capped at 99, so a tiny
 * change in a long string can never round up into the "100% verbatim" bucket.
 */
object AiMatchScorer {
  private val WHITESPACE = Regex("\\s+")

  fun score(
    aiText: String,
    finalText: String,
  ): Int {
    val a = tokenize(aiText)
    val b = tokenize(finalText)
    val max = maxOf(a.size, b.size)
    val dist = levenshtein(a, b)
    if (max == 0 || dist == 0) return 100
    val raw = (100.0 * (1.0 - dist.toDouble() / max)).roundToInt()
    return minOf(99, raw).coerceAtLeast(0)
  }

  private fun tokenize(text: String): List<String> =
    text
      .trim()
      .lowercase()
      .split(WHITESPACE)
      .filter { it.isNotEmpty() }

  private fun levenshtein(
    a: List<String>,
    b: List<String>,
  ): Int {
    if (a.isEmpty()) return b.size
    if (b.isEmpty()) return a.size

    var previous = IntArray(b.size + 1) { it }
    var current = IntArray(b.size + 1)
    for (i in 1..a.size) {
      current[0] = i
      for (j in 1..b.size) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
      }
      val tmp = previous
      previous = current
      current = tmp
    }
    return previous[b.size]
  }
}
