package io.tolgee.service.translation

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TmBatchBenchmarkTestData
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.service.translationMemory.TmAutoTranslateProviderOssImpl
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Microbenchmark for the per-item OSS TM lookup vs the batched
 * [TmAutoTranslateProviderOssImpl.getAutoTranslatedValuesForChunk] on the same data. Not part
 * of normal CI — runs the two paths back-to-back and prints timings so we can compare
 * main vs the fix.
 *
 * Disabled by default so it doesn't run on every CI build (it builds a 20k-key test project
 * and runs ~minute of timed work). Drop the [Disabled] annotation locally when you want
 * fresh numbers.
 *
 * Run with:
 *   ./gradlew :server-app:test --tests "io.tolgee.service.translation.TranslationMemoryServiceBenchmark" --console=plain
 */
@SpringBootTest
@Disabled("Microbenchmark — run manually when comparing per-item vs batched TM lookup timings.")
class TranslationMemoryServiceBenchmark : AbstractSpringTest() {
  @Autowired
  private lateinit var ossProvider: TmAutoTranslateProviderOssImpl

  private lateinit var testData: TmBatchBenchmarkTestData
  private lateinit var requestKeys: List<Key>

  private val warmupIterations = 1
  private val measuredIterations = 3

  @BeforeEach
  fun setUp() {
    // Big project: 20k TM source keys, 5k distinct base texts so each lookup has ~4 candidates on average.
    // 2k request keys to translate — much bigger than the production default chunk size, which is what
    // we want to stress (the cost of a real pretranslate_by_tm job over thousands of keys).
    testData = TmBatchBenchmarkTestData(sourceKeyCount = 20_000, requestKeyCount = 2_000, distinctSourceTexts = 5_000)
    testDataService.saveTestData(testData.root)
    requestKeys = testData.requestKeys.toList()
  }

  @Test
  fun `compare per-item vs batched TM lookups across chunk sizes`() {
    val germanLangId = testData.germanLanguage.id
    val germanLang = testData.germanLanguage

    // Warmup at chunk size 10 (no need to warm every size — JIT/connection pool warms up once)
    repeat(warmupIterations) {
      runPerItem(requestKeys, germanLang, chunkSize = 10)
      runBatched(requestKeys, germanLangId, chunkSize = 10)
    }

    println("============================================================")
    println("TM lookup benchmark")
    println("  Project: ${testData.sourceKeys.size} TM source keys, ${requestKeys.size} request keys to translate")
    println("  Distinct base texts: 5000 (~4 TM candidates per lookup on average)")
    println("============================================================")
    println("%-12s | %-30s | %-30s | %-8s".format("chunk size", "per-item (main)", "batched (this fix)", "speedup"))
    println("-".repeat(96))

    listOf(10, 50, 200).forEach { chunkSize ->
      val perItemTimes = mutableListOf<Long>()
      val batchedTimes = mutableListOf<Long>()
      repeat(measuredIterations) {
        perItemTimes += time { runPerItem(requestKeys, germanLang, chunkSize) }
        batchedTimes += time { runBatched(requestKeys, germanLangId, chunkSize) }
      }
      val perItemAvg = perItemTimes.average()
      val batchedAvg = batchedTimes.average()
      val speedup = perItemAvg / batchedAvg
      println(
        "%-12d | %-30s | %-30s | %.2fx".format(
          chunkSize,
          "${perItemTimes.map { "%4d".format(it) }} avg=${"%.0f".format(perItemAvg)} ms",
          "${batchedTimes.map { "%4d".format(it) }} avg=${"%.0f".format(batchedAvg)} ms",
          speedup,
        ),
      )
    }
    println("============================================================")
  }

  private fun runPerItem(
    keys: List<Key>,
    targetLanguage: Language,
    chunkSize: Int,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      keys.chunked(chunkSize).forEach { chunk ->
        chunk.forEach { key ->
          val attached = entityManager.find(Key::class.java, key.id)
          ossProvider.getAutoTranslatedValue(attached, targetLanguage)
        }
      }
    }
  }

  private fun runBatched(
    keys: List<Key>,
    targetLangId: Long,
    chunkSize: Int,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      keys.chunked(chunkSize).forEach { chunk ->
        ossProvider.getAutoTranslatedValuesForChunk(
          items = chunk.map { it.id to targetLangId },
          keysById = emptyMap(),
          languagesById = emptyMap(),
        )
      }
    }
  }

  private inline fun time(block: () -> Unit): Long {
    val start = System.nanoTime()
    block()
    return (System.nanoTime() - start) / 1_000_000
  }
}
