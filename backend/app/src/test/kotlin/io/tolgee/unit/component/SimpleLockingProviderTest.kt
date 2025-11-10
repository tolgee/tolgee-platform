package io.tolgee.unit.component

import io.tolgee.component.lockingProvider.SimpleLockingProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class SimpleLockingProviderTest {
  val provider = SimpleLockingProvider()

  @Test
  fun `it is locking`() {
    runBlocking {
      var num = 0

      val threads = 5
      val repeats = 10
      val expectedNum = threads * repeats

      withContext(Dispatchers.Default) {
        massiveRun(threads, repeats) {
          val theNum = num
          TimeUnit.MILLISECONDS.sleep(2L)
          num = theNum + 1
        }
      }
      println(num)
      assertThat(num).isLessThan(expectedNum)

      num = 0
      massiveRun(threads, repeats) {
        val lock = provider.getLock("yep")
        lock.lock()
        try {
          num++
        } finally {
          lock.unlock()
        }
      }
      assertThat(num).isEqualTo(expectedNum)
    }
  }

  @Test
  fun `locks are isolated`() {
    val yepLock = provider.getLock("yep")
    val yep2Lock = provider.getLock("yep2")

    yepLock.lock()
    yep2Lock.lock()
  }

  private suspend fun massiveRun(
    threads: Int,
    repeats: Int,
    action: suspend () -> Unit,
  ) {
    measureTimeMillis {
      coroutineScope {
        // scope for coroutines
        repeat(threads) {
          launch {
            repeat(repeats) { action() }
          }
        }
      }
    }
  }
}
