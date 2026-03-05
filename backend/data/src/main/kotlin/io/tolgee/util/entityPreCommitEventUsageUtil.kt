package io.tolgee.util

import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.events.OnEntityPreDelete
import io.tolgee.events.OnEntityPrePersist
import io.tolgee.events.OnEntityPreUpdate
import io.tolgee.model.SoftDeletable
import io.tolgee.model.translation.Translation

fun EntityPreCommitEvent.getUsageIncreaseAmount(): Long {
  return when (this) {
    is OnEntityPrePersist -> {
      1
    }

    is OnEntityPreDelete -> {
      -1
    }

    is OnEntityPreUpdate -> {
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

private fun OnEntityPreUpdate.getSoftDeleteUsageChange(): Long {
  if (entity !is SoftDeletable || propertyNames == null || previousState == null) return 0
  val deletedAtIndex = propertyNames!!.indexOf("deletedAt")
  if (deletedAtIndex < 0) return 0
  @Suppress("UNCHECKED_CAST")
  val oldValue = (previousState as Array<Any?>)[deletedAtIndex]
  val newValue = entity.deletedAt
  return when {
    oldValue == null && newValue != null -> -1 // soft-delete
    oldValue != null && newValue == null -> 1 // restore
    else -> 0
  }
}
