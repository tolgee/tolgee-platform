package io.tolgee.publicBilling

interface CloudSubscriptionModelProvider {
  fun provide(organizationId: Long): PublicCloudSubscriptionModel?
}
