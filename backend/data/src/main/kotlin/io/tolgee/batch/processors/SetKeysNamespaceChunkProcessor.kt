package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.SetKeysNamespaceRequest
import io.tolgee.constants.Message
import io.tolgee.model.batch.params.SetKeysNamespaceParams
import io.tolgee.service.key.KeyService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.apache.commons.lang3.exception.ExceptionUtils
import org.postgresql.util.PSQLException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class SetKeysNamespaceChunkProcessor(
  private val entityManager: EntityManager,
  private val keyService: KeyService,
  private val progressManager: ProgressManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<SetKeysNamespaceRequest, SetKeysNamespaceParams, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val subChunked = chunk.chunked(100)
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      catchingKeyAlreadyInNamespace {
        keyService.setNamespace(subChunk, params.namespace)
        entityManager.flush()
      }
      progressManager.reportSingleChunkProgress(job.id, subChunk.size)
    }
  }

  private fun catchingKeyAlreadyInNamespace(fn: () -> Unit) {
    try {
      fn.invoke()
    } catch (e: Exception) {
      if (violatesKeyUniqueness(e)) {
        throw FailedDontRequeueException(Message.KEY_EXISTS_IN_NAMESPACE, listOf(), e)
      }
      throw e
    }
  }

  /**
   * Not ConstraintViolationException.constraintName: Hibernate fills that in by searching the server
   * message for the English `violates unique constraint "`, so it is empty under a non-English
   * lc_messages.
   */
  fun violatesKeyUniqueness(e: Throwable) =
    ExceptionUtils
      .getThrowableList(e)
      .filterIsInstance<PSQLException>()
      .any { it.serverErrorMessage?.constraint in KEY_UNIQUENESS_INDEXES }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getTarget(data: SetKeysNamespaceRequest): List<Long> {
    return data.keyIds
  }

  override fun getParamsType(): Class<SetKeysNamespaceParams> {
    return SetKeysNamespaceParams::class.java
  }

  override fun getParams(data: SetKeysNamespaceRequest): SetKeysNamespaceParams {
    return SetKeysNamespaceParams().apply {
      this.namespace = data.namespace
    }
  }

  override fun getChunkSize(
    request: SetKeysNamespaceRequest,
    projectId: Long?,
  ): Int = 5000

  companion object {
    /** The unique indexes on `key` as created by schema.xml. */
    private val KEY_UNIQUENESS_INDEXES =
      setOf("key_project_branch_name_no_ns", "key_project_branch_name_ns")
  }
}
