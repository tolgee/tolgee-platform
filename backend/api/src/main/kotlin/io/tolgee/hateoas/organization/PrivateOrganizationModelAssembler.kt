package io.tolgee.hateoas.organization

import io.tolgee.constants.Feature
import io.tolgee.dtos.queryResults.organization.PrivateOrganizationView
import io.tolgee.hateoas.quickStart.QuickStartModelAssembler
import io.tolgee.publicBilling.CloudSubscriptionModelProvider
import org.springframework.stereotype.Component

@Component
class PrivateOrganizationModelAssembler(
  private val organizationModelAssembler: OrganizationModelAssembler,
  private val quickStartModelAssembler: QuickStartModelAssembler,
  private val cloudSubscriptionModelProvider: CloudSubscriptionModelProvider?,
) {
  fun toModel(
    view: PrivateOrganizationView,
    features: Array<Feature>,
  ): PrivateOrganizationModel {
    return PrivateOrganizationModel(
      organizationModel = organizationModelAssembler.toModel(view.organization),
      enabledFeatures = features,
      quickStart = view.quickStart?.let { quickStartModelAssembler.toModel(it) },
      activeCloudSubscription = cloudSubscriptionModelProvider?.provide(view.organization.id),
    )
  }
}
