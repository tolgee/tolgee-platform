package io.tolgee.hateoas.translations

import io.tolgee.model.enums.TaskType
import org.springframework.hateoas.RepresentationModel

open class TranslationTaskViewModel(
  val id: Long,
  val done: Boolean,
  val userAssigned: Boolean,
  val type: TaskType,
) : RepresentationModel<TranslationTaskViewModel>()
