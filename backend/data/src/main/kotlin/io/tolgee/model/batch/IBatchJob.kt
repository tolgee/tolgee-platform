package io.tolgee.model.batch

interface IBatchJob {
  var id: Long
  var status: BatchJobStatus
}
