package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.SetKeysNamespaceRequest
import io.tolgee.constants.Message
import io.tolgee.model.batch.params.SetKeysNamespaceParams
import io.tolgee.service.key.KeyService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import kotlinx.coroutines.ensureActive
import org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class SetKeysNamespaceChunkProcessor(
  private val entityManager: EntityManager,
  private val keyService: KeyService
) : ChunkProcessor<SetKeysNamespaceRequest, SetKeysNamespaceParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit
  ) {
    val subChunked = chunk.chunked(100)
    var progress = 0
    val params = getParams(job)
    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      try {
        keyService.setNamespace(subChunk, params.namespace)
        entityManager.flush()
      } catch (e: PersistenceException) {
        if (getRootCauseMessage(e).contains("key_project_id_name_namespace_id_idx")) {
          throw FailedDontRequeueException(Message.KEY_EXISTS_IN_NAMESPACE, listOf(), e)
        }
        throw e
      }
      progress += subChunk.size
      onProgress.invoke(progress)
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
