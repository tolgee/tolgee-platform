package io.tolgee.component.bucket

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class TokenBucketManager(
  val usingRedisProvider: UsingRedisProvider,
  val currentDateProvider: CurrentDateProvider,
  val lockingProvider: LockingProvider,
  @Lazy
  var redissonClient: RedissonClient
) : Logging {
  companion object {
    val localTokenBucketStorage = ConcurrentHashMap<String, TokenBucket>()
  }

  fun consume(bucketId: String, tokensToConsume: Long, bucketSize: Long, renewPeriod: Duration) {
    updateBucket(bucketId) {
      consumeMappingFn(
        tokenBucket = it,
        tokensToConsume = tokensToConsume,
        bucketSize = bucketSize,
        renewPeriod = renewPeriod
      )
    }
  }

  fun addTokens(bucketId: String, tokensToAdd: Long) {
    updateTokens(bucketId) {
      it + tokensToAdd
    }
  }

  fun updateTokens(bucketId: String, updateFn: ((oldTokens: Long) -> Long)) {
    updateBucket(bucketId) {
      updateMappingFn(it, updateFn)
    }
  }

  private fun getLockingId(bucketId: String) = "lock_bucket_$bucketId"

  private fun consumeMappingFn(
    tokenBucket: TokenBucket?,
    tokensToConsume: Long,
    bucketSize: Long,
    renewPeriod: Duration
  ): TokenBucket {
    val currentTokenBucket =
      getCurrentOrNewBucket(tokenBucket, bucketSize, renewPeriod)
    currentTokenBucket.refillIfItsTime(currentDateProvider.date.time, bucketSize)
    if (currentTokenBucket.tokens < tokensToConsume) {
      throw NotEnoughTokensException(currentTokenBucket.refillAt)
    }
    return currentTokenBucket.copy(tokens = currentTokenBucket.tokens - tokensToConsume)
  }

  private fun setEmptyUntilMappingFn(
    tokenBucket: TokenBucket?,
    emptyUntil: Long,
  ): TokenBucket? {
    tokenBucket ?: return null
    return tokenBucket.copy(tokens = 0, refillAt = emptyUntil)
  }

  private fun getCurrentOrNewBucket(
    tokenBucket: TokenBucket?,
    bucketSize: Long,
    renewPeriod: Duration
  ) = tokenBucket ?: TokenBucket(currentDateProvider.date.time, bucketSize, bucketSize, renewPeriod)

  private fun updateMappingFn(
    tokenBucket: TokenBucket?,
    updateFn: ((oldTokens: Long) -> Long)
  ): TokenBucket? {
    tokenBucket ?: return null
    val newTokens = updateFn(tokenBucket.tokens)
    return tokenBucket.copy(tokens = newTokens)
  }

  fun setEmptyUntil(bucketId: String, refillAt: Long) {
    logger.debug(
      "Setting bucket $bucketId empty for " +
        "next ${Duration.ofMillis(refillAt - currentDateProvider.date.time).seconds} seconds"
    )
    updateBucket(bucketId) { setEmptyUntilMappingFn(it, refillAt) }
  }

  fun updateBucket(bucketId: String, mappingFn: (bucket: TokenBucket?) -> TokenBucket?): TokenBucket? {
    if (!usingRedisProvider.areWeUsingRedis) {
      return localTokenBucketStorage.compute(bucketId) { _, bucket ->
        mappingFn(bucket)
      }
    }
    return lockingProvider.withLocking(getLockingId(bucketId)) {
      val redissonBucket = redissonClient.getBucket<TokenBucket>(bucketId)
      val newBucket = mappingFn(redissonBucket.get())
      redissonBucket.set(newBucket)
      return@withLocking newBucket
    }
  }
}
