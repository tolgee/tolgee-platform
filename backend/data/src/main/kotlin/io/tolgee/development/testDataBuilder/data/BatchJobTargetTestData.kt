package io.tolgee.development.testDataBuilder.data

import io.tolgee.batch.data.TranslationTargetProductSpec
import io.tolgee.model.batch.BatchJob

class BatchJobTargetTestData : BaseTestData() {
  val productJob: BatchJob =
    projectBuilder
      .addBatchJob {
        chunkSize = 2
        totalItems = 6
        totalChunks = 3
      }.build {
        targetProductProvider = { TranslationTargetProductSpec(listOf(1L, 2L), listOf(10L, 20L, 30L)) }
      }.self

  val listJob: BatchJob =
    projectBuilder
      .addBatchJob {
        chunkSize = 2
        totalItems = 3
        totalChunks = 2
      }.build {
        targetProvider = { listOf(7L, 8L, 9L) }
      }.self
}
