package io.tolgee.model.contentDelivery

interface AzureFrontDoorConfig : ContentDeliveryPurgingConfig {
  val clientId: String?
  val clientSecret: String?
  val tenantId: String?
  val contentRoot: String?
  val subscriptionId: String?
  val endpointName: String?
  val profileName: String?
  val resourceGroupName: String?

  override val contentDeliveryCachePurgingType: ContentDeliveryCachePurgingType
    get() = ContentDeliveryCachePurgingType.AZURE_FRONT_DOOR

  override val enabled: Boolean
    get() =
      arrayOf(clientId, clientSecret, tenantId, subscriptionId, profileName, endpointName, resourceGroupName)
        .all { it != null }
}
