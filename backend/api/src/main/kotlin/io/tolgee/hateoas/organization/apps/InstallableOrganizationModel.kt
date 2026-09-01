package io.tolgee.hateoas.organization.apps

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/** An organization a server admin can install an owned app into, as shown in the install picker. */
@Relation(collectionRelation = "installableOrganizations", itemRelation = "installableOrganization")
open class InstallableOrganizationModel(
  val id: Long,
  val name: String,
  val slug: String,
) : RepresentationModel<InstallableOrganizationModel>()
