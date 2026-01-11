package io.tolgee.util

fun <K, V> Map<K, V?>.filterValueNotNull(): Map<K, V> {
  return this.mapNotNull { entry -> entry.value?.let { entry.key to it } }.toMap()
}
