package io.tolgee.security.oauth2.cimd

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class CountingSlotsTest {
  @Test
  fun `a key that gave every slot back is dropped`() {
    val slots = CountingSlots(2)

    repeat(1000) { i ->
      slots.take("host-$i.example").assert.isTrue()
      slots.release("host-$i.example")
    }

    slots.trackedKeys().assert.isEqualTo(0)
  }

  @Test
  fun `releasing a key that holds nothing stores nothing`() {
    val slots = CountingSlots(2)

    slots.release("never-taken.example")

    slots.trackedKeys().assert.isEqualTo(0)
  }

  @Test
  fun `a refusal at the cap keeps the slots the key already holds`() {
    val slots = CountingSlots(2)

    repeat(2) { slots.take("busy.example").assert.isTrue() }
    repeat(3) { slots.take("busy.example").assert.isFalse() }
    slots.trackedKeys().assert.isEqualTo(1)

    slots.release("busy.example")
    slots.take("busy.example").assert.isTrue()
    slots.take("busy.example").assert.isFalse()
  }
}
