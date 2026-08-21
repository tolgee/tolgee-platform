package io.tolgee.hateoas.organization.apps

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/** An organization that currently has an owned app installed, shown in the installations view. */
@Relation(collectionRelation = "installingOrganizations", itemRelation = "installingOrganization")
open class InstallingOrganizationModel(
  val id: Long,
  val name: String,
  val slug: String,
) : RepresentationModel<InstallingOrganizationModel>()
