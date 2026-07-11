package io.tolgee.hateoas

import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel

data class TranslationAgencySimpleModel(
  var id: Long = 0L,
  var name: String = "",
  var url: String? = "",
  val avatar: Avatar?,
) : RepresentationModel<TranslationAgencySimpleModel>()
