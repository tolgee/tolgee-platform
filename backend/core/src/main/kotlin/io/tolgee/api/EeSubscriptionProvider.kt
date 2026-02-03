package io.tolgee.api

interface EeSubscriptionProvider {
  fun findSubscriptionDto(): EeSubscriptionDto?

  fun getLicensingUrl(): String?
}
