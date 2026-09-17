package io.tolgee.security.oauth2.cimd

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class CimdFetchBudgetTest {
  @Test
  fun `one slow origin cannot hold every slot`() {
    val budget = CimdFetchBudget(maxConcurrent = 10, maxConcurrentPerOrigin = 2)

    parked(budget, "https://slow.example/a", 2) {
      budget.admitted("https://slow.example/b").assert.isFalse()
    }
  }

  @Test
  fun `an origin at its cap does not refuse another origin`() {
    val budget = CimdFetchBudget(maxConcurrent = 10, maxConcurrentPerOrigin = 2)

    parked(budget, "https://slow.example/a", 2) {
      budget.admitted("https://other.example/a").assert.isTrue()
    }
  }

  @Test
  fun `the global cap binds across origins`() {
    val budget = CimdFetchBudget(maxConcurrent = 2, maxConcurrentPerOrigin = 5)

    parked(budget, "https://a.example/x", 1) {
      parked(budget, "https://b.example/x", 1) {
        budget.admitted("https://c.example/x").assert.isFalse()
      }
    }
  }

  @Test
  fun `slots come back, so the instance does not run out over its lifetime`() {
    val budget = CimdFetchBudget(maxConcurrent = 2, maxConcurrentPerOrigin = 2)

    repeat(20) { budget.admitted("https://app.example/client").assert.isTrue() }
  }

  @Test
  fun `a refusal by the global cap gives the origin slot back`() {
    // Otherwise the per-origin counters ratchet upward and that origin is denied permanently.
    val budget = CimdFetchBudget(maxConcurrent = 1, maxConcurrentPerOrigin = 5)

    parked(budget, "https://a.example/x", 1) {
      budget.admitted("https://b.example/x").assert.isFalse()
    }
    budget.admitted("https://b.example/x").assert.isTrue()
  }

  @Test
  fun `a client_id naming another port on the same host gets its own bucket`() {
    // Otherwise a hostile client_id on a filtered port of a legitimate publisher's host parks connect timeouts in
    // that publisher's bucket and denies every cold authorization of it.
    val budget = CimdFetchBudget(maxConcurrent = 10, maxConcurrentPerOrigin = 1)

    parked(budget, "https://app.example:8443/evil", 1) {
      budget.admitted("https://app.example/client").assert.isTrue()
    }
  }

  @Test
  fun `the default port and a spelled-out one are the same bucket`() {
    val budget = CimdFetchBudget(maxConcurrent = 10, maxConcurrentPerOrigin = 1)

    parked(budget, "https://app.example:443/evil", 1) {
      budget.admitted("https://app.example/client").assert.isFalse()
    }
  }

  @Test
  fun `a client_id with no parseable host is refused without consuming anything`() {
    val budget = CimdFetchBudget(maxConcurrent = 1, maxConcurrentPerOrigin = 1)

    budget.admitted("not a url").assert.isFalse()
    budget.admitted("https://app.example/client").assert.isTrue()
  }

  @Test
  fun `one origin cannot be sent unbounded fetches, however many client_ids it is given`() {
    val clock = AtomicLong(0)
    val budget = CimdFetchBudget(maxFetchesPerOriginPerMinute = 3, ticker = clock::get)

    repeat(3) { budget.admitted("https://victim.example/$it").assert.isTrue() }
    budget.admitted("https://victim.example/4").assert.isFalse()
    budget.admitted("https://other.example/4").assert.isTrue()

    clock.set(TimeUnit.MINUTES.toNanos(2))
    budget.admitted("https://victim.example/5").assert.isTrue()
  }

  @Test
  fun `a request the concurrency cap turned away does not spend the origin's quota`() {
    // The parked call itself is charged, so two of the three are left for after it - and only if the five the
    // concurrency cap refused cost the origin nothing.
    val budget = CimdFetchBudget(maxConcurrent = 10, maxConcurrentPerOrigin = 1, maxFetchesPerOriginPerMinute = 3)

    parked(budget, "https://victim.example/a", 1) {
      repeat(5) { budget.admitted("https://victim.example/b").assert.isFalse() }
    }

    repeat(2) { budget.admitted("https://victim.example/c").assert.isTrue() }
    budget.admitted("https://victim.example/d").assert.isFalse()
  }

  private fun CimdFetchBudget.admitted(clientIdUrl: String): Boolean = withBudget(clientIdUrl) { true } != null

  private fun parked(
    budget: CimdFetchBudget,
    clientIdUrl: String,
    count: Int,
    whileHeld: () -> Unit,
  ) {
    val release = CountDownLatch(1)
    val holding = CountDownLatch(count)
    val pool = Executors.newFixedThreadPool(count)
    repeat(count) {
      pool.submit {
        budget.withBudget(clientIdUrl) {
          holding.countDown()
          release.await()
        }
      }
    }
    holding.await(10, TimeUnit.SECONDS).assert.isTrue()
    try {
      whileHeld()
    } finally {
      release.countDown()
      pool.shutdown()
      pool.awaitTermination(10, TimeUnit.SECONDS)
    }
  }
}
