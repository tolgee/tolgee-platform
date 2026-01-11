package io.tolgee.component.bucket

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.util.Logging
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

@Component
class TokenBucketManager(
  val usingRedisProvider: UsingRedisProvider,
  val currentDateProvider: CurrentDateProvider,
  val lockingProvider: LockingProvider,
  @Lazy
  var redissonClient: RedissonClient,
) : Logging {
  companion object {
    val localTokenBucketStorage = ConcurrentHashMap<String, TokenBucket>()
  }

  fun consume(
    bucketId: String,
    tokensToConsume: Long,
    bucketSize: Long,
    renewPeriod: Duration,
  ) {
    updateBucket(bucketId) {
      consumeMappingFn(
        tokenBucket = it,
        tokensToConsume = tokensToConsume,
        bucketSize = bucketSize,
        renewPeriod = renewPeriod,
      )
    }
  }

  fun checkPositiveBalance(bucketId: String) {
    updateBucket(bucketId) {
      checkPositiveMappingFn(
        tokenBucket = it,
      )
    }
  }

  fun addTokens(
    bucketId: String,
    tokensToAdd: Long,
  ) {
    updateTokens(bucketId) { oldTokens, bucketSize ->
      min(oldTokens + tokensToAdd, bucketSize)
    }
  }

  fun updateTokens(
    bucketId: String,
    updateFn: ((oldTokens: Long, bucketSize: Long) -> Long),
  ) {
    updateBucket(bucketId) {
      updateMappingFn(it, updateFn)
    }
  }

  fun setEmptyUntil(
    bucketId: String,
    refillAt: Long,
  ) {
    updateBucket(bucketId) { setEmptyUntilMappingFn(it, refillAt) }
  }

  private fun getLockingId(bucketId: String) = "lock_bucket_$bucketId"

  private fun consumeMappingFn(
    tokenBucket: TokenBucket?,
    tokensToConsume: Long,
    bucketSize: Long,
    renewPeriod: Duration,
  ): TokenBucket {
    val currentTokenBucket =
      getCurrentOrNewBucket(tokenBucket, bucketSize, renewPeriod)
    currentTokenBucket.refillIfItsTime(currentDateProvider.date.time, bucketSize, renewPeriod)
    if (currentTokenBucket.tokens < tokensToConsume) {
      throw NotEnoughTokensException(currentTokenBucket.refillAt)
    }
    return currentTokenBucket.apply { tokens = currentTokenBucket.tokens - tokensToConsume }
  }

  private fun checkPositiveMappingFn(tokenBucket: TokenBucket?): TokenBucket? {
    tokenBucket ?: return null
    if (tokenBucket.isTimeToRefill(currentDateProvider.date.time)) {
      return tokenBucket
    }
    if (tokenBucket.tokens <= 0) {
      throw NotEnoughTokensException(tokenBucket.refillAt)
    }
    return tokenBucket
  }

  private fun setEmptyUntilMappingFn(
    tokenBucket: TokenBucket?,
    emptyUntil: Long,
  ): TokenBucket {
    val currentBucket = getCurrentOrNewBucket(tokenBucket, 0, Duration.ZERO)
    return currentBucket.apply {
      tokens = 0
      refillAt = emptyUntil
    }
  }

  private fun getCurrentOrNewBucket(
    tokenBucket: TokenBucket?,
    bucketSize: Long,
    renewPeriod: Duration,
  ) = tokenBucket ?: TokenBucket(currentDateProvider.date.time, bucketSize, bucketSize, renewPeriod)

  private fun updateMappingFn(
    tokenBucket: TokenBucket?,
    updateFn: ((oldTokens: Long, bucketSize: Long) -> Long),
  ): TokenBucket? {
    tokenBucket ?: return null
    val newTokens = updateFn(tokenBucket.tokens, tokenBucket.size)
    return tokenBucket.apply { tokens = newTokens }
  }

  private fun updateBucket(
    bucketId: String,
    mappingFn: (bucket: TokenBucket?) -> TokenBucket?,
  ): TokenBucket? {
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
