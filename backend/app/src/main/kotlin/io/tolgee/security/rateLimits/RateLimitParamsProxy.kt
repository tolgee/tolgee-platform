package io.tolgee.security.rateLimits

import org.springframework.stereotype.Component

@Component
/**
 * This class just allow us to mock the bucket params for testing
 *
 * In tests, we can just mock this bean and set custom values without restarting spring server
 */
class RateLimitParamsProxy {

  fun getTimeToRefill(keyPrefix: String, timeToRefill: Int): Int {
    return timeToRefill
  }

  fun getBucketSize(keyPrefix: String, bucketSize: Int): Int {
    return bucketSize
  }
}
