package io.tolgee.util

import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.events.OnEntityPreDelete
import io.tolgee.events.OnEntityPrePersist
import io.tolgee.events.OnEntityPreUpdate
import io.tolgee.model.SoftDeletable
import io.tolgee.model.translation.Translation

fun EntityPreCommitEvent<*>.getWordUsageIncreaseAmount(): Long {
  val translation = entity as? Translation ?: return 0
  val wordCount = (translation.wordCount ?: 0).toLong()
  return when (this) {
    is OnEntityPrePersist<*> -> wordCount

    is OnEntityPreDelete<*> -> -wordCount

    is OnEntityPreUpdate<*> -> getWordCountDelta(wordCount)

    else -> 0
  }
}

private fun OnEntityPreUpdate<*>.getWordCountDelta(newWordCount: Long): Long {
  if (previousState == null) return 0
  val wordCountIndex = propertyNames?.indexOf("wordCount") ?: -1
  if (wordCountIndex < 0) return 0
  val oldWordCount = (previousState?.getOrNull(wordCountIndex) as? Int)?.toLong() ?: 0L
  return newWordCount - oldWordCount
}

fun EntityPreCommitEvent<*>.getUsageIncreaseAmount(): Long {
  return when (this) {
    is OnEntityPrePersist<*> -> {
      1
    }

    is OnEntityPreDelete<*> -> {
      -1
    }

    is OnEntityPreUpdate<*> -> {
      val softDeleteChange = getSoftDeleteUsageChange()
      if (softDeleteChange != 0L) return softDeleteChange

      val entity = this.entity
      if (entity is Translation) {
        val textIndex = this.propertyNames?.indexOf("text") ?: -1
        if (textIndex != -1) {
          val previousText = this.previousState?.get(textIndex) as? String
          val currentText = entity.text
          if (previousText.isNullOrEmpty() && !currentText.isNullOrEmpty()) {
            return 1
          } else if (!previousText.isNullOrEmpty() && currentText.isNullOrEmpty()) {
            return -1
          }
        }
      }
      0
    }

    else -> {
      0
    }
  }
}

private fun OnEntityPreUpdate<*>.getSoftDeleteUsageChange(): Long {
  val current = entity
  if (current !is SoftDeletable || propertyNames == null || previousState == null) return 0
  val deletedAtIndex = propertyNames!!.indexOf("deletedAt")
  if (deletedAtIndex < 0) return 0
  @Suppress("UNCHECKED_CAST")
  val oldValue = (previousState as Array<Any?>)[deletedAtIndex]
  val newValue = current.deletedAt
  return when {
    oldValue == null && newValue != null -> -1 // soft-delete
    oldValue != null && newValue == null -> 1 // restore
    else -> 0
  }
}
