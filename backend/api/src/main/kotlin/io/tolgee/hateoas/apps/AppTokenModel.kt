package io.tolgee.hateoas.apps

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(itemRelation = "appToken")
open class AppTokenModel(
  val token: String,
) : RepresentationModel<AppTokenModel>()
