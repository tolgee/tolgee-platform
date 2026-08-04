package io.tolgee.hateoas.project.apps

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(itemRelation = "appToken")
open class AppTokenModel(
  val token: String,
) : RepresentationModel<AppTokenModel>()
