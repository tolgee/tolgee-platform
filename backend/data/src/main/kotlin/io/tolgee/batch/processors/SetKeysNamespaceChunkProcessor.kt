package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.SetKeysNamespaceRequest
import io.tolgee.constants.Message
import io.tolgee.model.batch.params.SetKeysNamespaceParams
import io.tolgee.service.key.KeyService
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class SetKeysNamespaceChunkProcessor(
  private val entityManager: EntityManager,
  private val keyService: KeyService,
) : ChunkProcessor<SetKeysNamespaceRequest, SetKeysNamespaceParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val subChunked = chunk.chunked(100)
    var progress = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      catchingKeyAlreadyInNamespace {
        keyService.setNamespace(subChunk, params.namespace)
        entityManager.flush()
      }
      progress += subChunk.size
      onProgress.invoke(progress)
    }
  }

  private fun catchingKeyAlreadyInNamespace(fn: () -> Unit) {
    try {
      fn.invoke()
    } catch (e: Exception) {
      val rootCause = getRootCauseMessage(e)
      val isKeyAlreadyInNamespace =
        rootCause
          .contains("key_project_id_name_namespace_id_idx")
      val isKeyAlreadyInProjectWithoutNamespace =
        rootCause
          .contains("key_project_id_name_idx")
      if (isKeyAlreadyInNamespace || isKeyAlreadyInProjectWithoutNamespace) {
        throw FailedDontRequeueException(Message.KEY_EXISTS_IN_NAMESPACE, listOf(), e)
      }
      throw e
    }
  }

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
}
