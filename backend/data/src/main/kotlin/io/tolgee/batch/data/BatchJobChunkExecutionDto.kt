package io.tolgee.batch.data

import io.tolgee.batch.JobCharacter
import java.util.Date

/**
 * DTO object for the BatchJobChunkExecution. Contains the bare minimum needed for the
 * BatchJobChunckExecutionQueue.
 *
 * @author Geert Zondervan <zondervan@serviceplanet.nl>
 */
class BatchJobChunkExecutionDto(
  val id: Long,
  val batchJobId: Long,
  var executeAfter: Date?,
  val jobCharacter: JobCharacter,
)
