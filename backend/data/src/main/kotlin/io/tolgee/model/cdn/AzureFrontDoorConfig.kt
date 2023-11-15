package io.tolgee.model.cdn

interface AzureFrontDoorConfig : CdnPurgingConfig {
  val clientId: String?
  val clientSecret: String?
  val tenantId: String?
  val contentRoot: String?
  val subscriptionId: String?
  val endpointName: String?
  val profileName: String?
  val resourceGroupName: String?

  override val cdnPurgingType: CdnPurgingType get() = CdnPurgingType.AZURE_FRONT_DOOR

  override val enabled: Boolean
    get() = arrayOf(clientId, clientSecret, tenantId, subscriptionId, profileName, endpointName, resourceGroupName)
      .all { it != null }
}
