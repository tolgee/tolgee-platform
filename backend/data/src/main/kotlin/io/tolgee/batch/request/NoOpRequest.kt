package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty

class NoOpRequest {
  @NotEmpty
  var itemIds: List<Long> = listOf()
  var chunkProcessingDelayMs: Long = 0
}

class NoOpMultiRequest {
  var totalItems: Int = 10000
  var numberOfJobs: Int = 1
  var chunkProcessingDelayMs: Long = 0
  var numberOfProjects: Int = 0
}
