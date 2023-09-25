package io.tolgee.hateoas.organization

import io.tolgee.constants.Feature
import io.tolgee.hateoas.quickStart.QuickStartModelAssembler
import io.tolgee.model.QuickStart
import io.tolgee.model.views.OrganizationView
import org.springframework.stereotype.Component

@Component
class PrivateOrganizationModelAssembler(
  private val organizationModelAssembler: OrganizationModelAssembler,
  private val quickStartModelAssembler: QuickStartModelAssembler
) {
  fun toModel(
    organization: OrganizationView,
    features: Array<Feature>,
    quickStart: QuickStart?
  ): PrivateOrganizationModel {
    return PrivateOrganizationModel(
      organizationModel = organizationModelAssembler.toModel(organization),
      enabledFeatures = features,
      quickStart = quickStart?.let { quickStartModelAssembler.toModel(it) }
    )
  }
}
