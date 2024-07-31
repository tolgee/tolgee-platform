package io.tolgee.hateoas.translations

import io.tolgee.model.enums.TaskState
import org.springframework.hateoas.RepresentationModel

open class TranslationTaskViewModel (
  val id: Long,
  val done: Boolean,
  val userAssigned: Boolean,
): RepresentationModel<TranslationTaskViewModel>()
