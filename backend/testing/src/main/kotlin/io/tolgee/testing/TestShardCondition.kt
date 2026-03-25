package io.tolgee.testing

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.math.absoluteValue

/**
 * JUnit 5 extension that enables test sharding by distributing test classes
 * across shards based on class name hash.
 *
 * Controlled by system properties:
 * - `tolgee.test.shard.index` — the current shard (0-based)
 * - `tolgee.test.shard.total` — total number of shards
 *
 * When not set, all tests run (no sharding).
 */
class TestShardCondition : ExecutionCondition {
  override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
    val shardTotal = System.getProperty("tolgee.test.shard.total")?.toIntOrNull()
      ?: return ConditionEvaluationResult.enabled("No sharding configured")
    val shardIndex = System.getProperty("tolgee.test.shard.index")?.toIntOrNull()
      ?: return ConditionEvaluationResult.enabled("No shard index configured")

    if (shardTotal <= 1) {
      return ConditionEvaluationResult.enabled("Single shard")
    }

    // Only filter at class level — don't re-evaluate for each method
    val testClass = context.testClass.orElse(null)
      ?: return ConditionEvaluationResult.enabled("Not a class context")

    val hash = testClass.name.hashCode().absoluteValue % shardTotal
    return if (hash == shardIndex) {
      ConditionEvaluationResult.enabled("Class ${testClass.simpleName} assigned to shard $shardIndex")
    } else {
      ConditionEvaluationResult.disabled("Class ${testClass.simpleName} assigned to shard $hash, skipping in shard $shardIndex")
    }
  }
}
