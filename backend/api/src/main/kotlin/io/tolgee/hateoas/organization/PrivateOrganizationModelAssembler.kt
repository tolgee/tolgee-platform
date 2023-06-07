package io.tolgee.hateoas.organization

import io.tolgee.api.v2.controllers.organization.OrganizationController
import io.tolgee.constants.Feature
import io.tolgee.model.views.OrganizationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PrivateOrganizationModelAssembler(
  private val organizationModelAssembler: OrganizationModelAssembler
) : RepresentationModelAssemblerSupport<Pair<OrganizationView, Array<Feature>>, PrivateOrganizationModel>(
  OrganizationController::class.java, PrivateOrganizationModel::class.java
) {
  override fun toModel(data: Pair<OrganizationView, Array<Feature>>): PrivateOrganizationModel {
    val (view, features) = data
    return PrivateOrganizationModel(
      organizationModel = organizationModelAssembler.toModel(view),
      enabledFeatures = features
    )
  }
}
