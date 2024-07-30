package io.tolgee.model.task

import io.tolgee.model.translation.Translation
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import java.io.Serializable

data class TaskTranslationId(
  @ManyToOne(fetch = FetchType.LAZY)
  var task: Task = Task(),
  @ManyToOne(fetch = FetchType.LAZY)
  var translation: Translation = Translation(),
) : Serializable
