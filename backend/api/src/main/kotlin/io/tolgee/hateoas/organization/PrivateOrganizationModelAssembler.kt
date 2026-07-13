package io.tolgee.hateoas.organization

import io.tolgee.constants.Feature
import io.tolgee.dtos.queryResults.organization.PrivateOrganizationView
import io.tolgee.hateoas.quickStart.QuickStartModelAssembler
import io.tolgee.publicBilling.CloudSubscriptionModelProvider
import io.tolgee.publicBilling.PublicCloudSubscriptionModel
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
    isAtLeastMember: Boolean,
    limitedView: Boolean,
  ): PrivateOrganizationModel {
    val organizationId = view.organization.id
    val organizationModel = organizationModelAssembler.toModel(view.organization)
    return PrivateOrganizationModel(
      organizationModel = organizationModel,
      enabledFeatures = features,
      quickStart = view.quickStart?.let { quickStartModelAssembler.toModel(it) },
      activeCloudSubscription = activeCloudSubscription(isAtLeastMember, organizationId),
      limitedView = limitedView,
    )
  }

  private fun activeCloudSubscription(
    isAtLeastMember: Boolean,
    organizationId: Long,
  ): PublicCloudSubscriptionModel? {
    if (!isAtLeastMember) {
      return null
    }
    return cloudSubscriptionModelProvider?.provide(organizationId)
  }
}
