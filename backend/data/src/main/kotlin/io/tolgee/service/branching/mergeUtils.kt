package io.tolgee.service.branching

import io.tolgee.model.enums.BranchKeyMergeResolutionType

fun <T> chooseThreeWay(
  source: T?,
  target: T?,
  base: T?,
  resolution: BranchKeyMergeResolutionType,
): T? =
  when {
    source == target -> source
    source == base -> target
    target == base -> source
    source == null -> target
    target == null -> source
    base == null -> if (resolution == BranchKeyMergeResolutionType.SOURCE) source else target
    else -> if (resolution == BranchKeyMergeResolutionType.SOURCE) source else target
  }

fun <T> isConflictingThreeWay(
  source: T?,
  target: T?,
  base: T?,
): Boolean = source != target && source != base && target != base

fun <T> mergeSetsWithBase(
  snapshot: Set<T>,
  source: Set<T>,
  target: Set<T>,
): Set<T> {
  val removedBySource = snapshot - source
  val removedByTarget = snapshot - target
  return (source + target) - (removedBySource + removedByTarget)
}

fun <K, V> mergeByKey(
  snapshot: Map<K, V>,
  source: Map<K, V>,
  target: Map<K, V>,
  resolution: BranchKeyMergeResolutionType,
): Map<K, V> {
  val keys = snapshot.keys + source.keys + target.keys
  return buildMap {
    keys.forEach { key ->
      val value =
        chooseThreeWay(
          source = source[key],
          target = target[key],
          base = snapshot[key],
          resolution = resolution,
        )
      if (value != null) {
        put(key, value)
      }
    }
  }
}
