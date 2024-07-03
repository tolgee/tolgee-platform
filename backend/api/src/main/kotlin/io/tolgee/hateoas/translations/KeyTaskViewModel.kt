package io.tolgee.hateoas.translations

import io.tolgee.model.enums.TaskType
import org.springframework.hateoas.RepresentationModel

open class KeyTaskViewModel(
  val number: Long,
  val languageId: Long,
  val languageTag: String,
  val done: Boolean,
  val userAssigned: Boolean,
  val type: TaskType,
) : RepresentationModel<KeyTaskViewModel>()
