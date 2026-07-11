package io.tolgee.hateoas.auth

import org.springframework.hateoas.RepresentationModel

class AuthInfoModel(
  val isReadOnly: Boolean,
) : RepresentationModel<AuthInfoModel>()
