package io.tolgee.unit

import io.tolgee.testing.TestShardCondition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class TestShardConditionTest {
  private val condition = TestShardCondition()
  private val properties = listOf("tolgee.test.shard.index", "tolgee.test.shard.total")

  // this class itself runs inside a sharded JVM whose properties every later class still reads
  private lateinit var jvmShard: Map<String, String?>

  @BeforeEach
  fun saveJvmShard() {
    jvmShard = properties.associateWith { System.getProperty(it) }
    properties.forEach { System.clearProperty(it) }
  }

  @AfterEach
  fun restoreJvmShard() {
    jvmShard.forEach { (name, value) ->
      if (value == null) {
        System.clearProperty(name)
        return@forEach
      }
      System.setProperty(name, value)
    }
  }

  @Test
  fun `runs everything when no shard is configured`() {
    assertThat(isDisabled(Outer::class.java)).isFalse
  }

  @Test
  fun `runs a class only in the shard its name hashes to`() {
    val total = 3
    val own = (Outer::class.java.name.hashCode() and Int.MAX_VALUE) % total
    System.setProperty("tolgee.test.shard.total", total.toString())
    (0 until total).forEach { shard ->
      System.setProperty("tolgee.test.shard.index", shard.toString())
      assertThat(isDisabled(Outer::class.java)).isEqualTo(shard != own)
    }
  }

  @Test
  fun `a nested class runs in the shard of its outer class`() {
    val total = 97
    System.setProperty("tolgee.test.shard.total", total.toString())
    (0 until total).forEach { shard ->
      System.setProperty("tolgee.test.shard.index", shard.toString())
      assertThat(isDisabled(Outer.Inner::class.java)).isEqualTo(isDisabled(Outer::class.java))
    }
  }

  private fun isDisabled(testClass: Class<*>): Boolean {
    val context = mock<ExtensionContext>()
    whenever(context.testClass).thenReturn(Optional.of(testClass))
    return condition.evaluateExecutionCondition(context).isDisabled
  }
}

private class Outer {
  class Inner
}
