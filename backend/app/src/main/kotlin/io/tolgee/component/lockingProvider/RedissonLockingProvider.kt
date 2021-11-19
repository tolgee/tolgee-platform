package io.tolgee.component.lockingProvider

import org.redisson.api.RedissonClient
import java.util.concurrent.locks.Lock

class RedissonLockingProvider(private val redissonClient: RedissonClient) : LockingProvider {
  override fun getLock(name: String): Lock {
    return redissonClient.getLock(name)
  }
}
