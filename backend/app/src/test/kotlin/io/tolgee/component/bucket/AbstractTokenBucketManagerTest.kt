package io.tolgee.component.bucket

import io.tolgee.component.CurrentDateProvider
import io.tolgee.testing.assert
import io.tolgee.util.addMinutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

abstract class AbstractTokenBucketManagerTest {
  @Autowired
  lateinit var tokenBucketManager: TokenBucketManager

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = currentDateProvider.date
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun `it consumes credits`() {
    val bucketId = "test"
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 20, Duration.ofMinutes(1))
    }

    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(1)

    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 20, Duration.ofMinutes(1))
    }
  }

  @Test
  fun `it checks positive balance`() {
    // for new bucket it should be ok
    val bucketId = "test2"
    tokenBucketManager.checkPositiveBalance(bucketId) // OK

    // this initializes new bucket with 20 tokens, half of them are consumed
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK

    tokenBucketManager.checkPositiveBalance(bucketId) // OK

    // this consumes the rest
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK

    // now we don't have positive balance
    assertThrowsRetry { tokenBucketManager.checkPositiveBalance(bucketId) }
  }

  @Test
  fun `it adds tokens`() {
    val bucketId = "test3"
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK
    tokenBucketManager.addTokens(bucketId, 15)

    // it doesn't add more than bucket capacity
    // this empties the bucket
    tokenBucketManager.consume(bucketId, 20, 20, Duration.ofMinutes(1)) // OK

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 20, Duration.ofMinutes(1))
    }

    tokenBucketManager.addTokens(bucketId, 19)

    // it does add under the capacity
    // this empties the bucket
    tokenBucketManager.consume(bucketId, 19, 20, Duration.ofMinutes(1)) // OK

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 20, Duration.ofMinutes(1))
    }
  }

  @Test
  fun `updates tokens`() {
    val bucketId = "test4"

    // it works only on initialized bucket, otherwise it doesn't make sense
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK

    tokenBucketManager.updateTokens(bucketId) { oldTokens, _ ->
      oldTokens * 3
    }

    // we can go over capacity
    tokenBucketManager.consume(bucketId, 30, 20, Duration.ofMinutes(1)) // OK

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 20, Duration.ofMinutes(1))
    }
  }

  @Test
  fun `it is thread safe`() {
    val bucketId = "test5"

    // it works only on initialized bucket, otherwise it doesn't make sense
    tokenBucketManager.consume(bucketId, 0, 1000, Duration.ofMinutes(1)) // OK

    (1..10).forEach {
      runBlocking {
        (1..100).forEach {
          launch {
            // it works only on initialized bucket, otherwise it doesn't make sense
            tokenBucketManager.consume(bucketId, 1, 1000, Duration.ofMinutes(1)) // OK
          }
        }
      }
    }

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 1000, Duration.ofMinutes(1))
    }
  }

  @Test
  fun `sets empty even when not initialized`() {
    val bucketId = "test6"
    tokenBucketManager.setEmptyUntil(bucketId, currentDateProvider.date.addMinutes(1).time)

    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 10, Duration.ofMinutes(1))
    }

    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(1)

    // this initializes the bucket
    tokenBucketManager.consume(bucketId, 1, 10, Duration.ofMinutes(1))

    // this consumes the rest
    tokenBucketManager.consume(bucketId, 9, 10, Duration.ofMinutes(1))

    // not it throws again
    assertThrowsRetry {
      tokenBucketManager.consume(bucketId, 1, 10, Duration.ofMinutes(1))
    }

    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(1)

    // we can consume again in 1 minute
    tokenBucketManager.consume(bucketId, 10, 10, Duration.ofMinutes(1))
  }

  @Test
  fun `empties the bucket until specific time`() {
    val bucketId = "test7"

    // it works only on initialized bucket, otherwise it doesn't make sense
    tokenBucketManager.consume(bucketId, 10, 20, Duration.ofMinutes(1)) // OK

    tokenBucketManager.setEmptyUntil(bucketId, currentDateProvider.date.addMinutes(10).time)

    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(3)

    assertThrowsRetry(Duration.ofMinutes(7)) {
      // we can go over capacity
      tokenBucketManager.consume(bucketId, 1, 20, Duration.ofMinutes(1)) // OK
    }
  }

  private fun assertThrowsRetry(
    expectedRetryTime: Duration = Duration.ofMinutes(1),
    fn: () -> Unit,
  ) {
    val refillAt =
      assertThrows<NotEnoughTokensException> {
        fn()
      }.refillAt

    val expectedRefillAt = currentDateProvider.date.time + expectedRetryTime.toMillis()

    refillAt.assert
      .overridingErrorMessage {
        val difference = Duration.ofMillis(refillAt - expectedRefillAt)
        "RefillAt is not correct. Difference is ${difference.seconds} seconds."
      }.isEqualTo(expectedRefillAt)
  }
}
