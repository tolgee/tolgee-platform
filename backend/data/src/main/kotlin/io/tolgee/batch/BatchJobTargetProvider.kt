package io.tolgee.batch

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.TranslationTargetProduct
import io.tolgee.batch.data.TranslationTargetProductSpec
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.batch.BatchJob
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * Targets can have millions of items. Keep them out of the job entity / DTO, which are loaded and cached (also in
 * Redis) on every chunk.
 */
@Component
class BatchJobTargetProvider(
  private val entityManager: EntityManager,
  private val objectMapper: ObjectMapper,
) {
  private val cache: Cache<Long, List<Any>> =
    Caffeine
      .newBuilder()
      .maximumWeight(MAX_CACHED_ITEMS)
      .weigher<Long, List<Any>> { _, target -> weight(target) }
      .expireAfterAccess(Duration.ofMinutes(30))
      .build()

  fun getTarget(jobId: Long): List<Any> = cache.get(jobId) { load(it) }

  fun getChunk(
    job: BatchJobDto,
    chunkNumber: Int,
  ): List<Any> = BatchJob.getChunk(job.chunkSize, getTarget(job.id), chunkNumber)

  fun cacheTarget(
    jobId: Long,
    target: List<Any>,
  ) {
    cache.put(jobId, target)
  }

  fun evict(jobId: Long) {
    cache.invalidate(jobId)
  }

  private fun load(jobId: Long): List<Any> {
    val row =
      entityManager
        .createNativeQuery(
          "select cast(target as text), cast(target_product as text) from tolgee_batch_job where id = :id",
        ).setParameter("id", jobId)
        .resultList
        .firstOrNull() as Array<*>?
        ?: throw NotFoundException(Message.BATCH_JOB_NOT_FOUND)

    val product = row[1] as String?
    if (product != null) {
      return objectMapper.readValue(product, TranslationTargetProductSpec::class.java).toTarget()
    }

    val target = row[0] as String? ?: return emptyList()
    @Suppress("UNCHECKED_CAST")
    return objectMapper.readValue(target, List::class.java) as List<Any>
  }

  private fun weight(target: List<Any>): Int {
    if (target is TranslationTargetProduct) {
      return target.keyIds.size + target.languageIds.size
    }
    return target.size.coerceAtLeast(1)
  }

  companion object {
    private const val MAX_CACHED_ITEMS = 20_000_000L
  }
}
