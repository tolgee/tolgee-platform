package io.tolgee.hateoas.userAccount

import io.tolgee.dtos.Avatar
import org.springframework.hateoas.RepresentationModel

data class SimpleUserAccountModel(
  val id: Long,
  val username: String,
  var name: String?,
  var avatar: Avatar?,
  var deleted: Boolean,
) : RepresentationModel<SimpleUserAccountModel>()
