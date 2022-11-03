package io.tolgee.component.publicBillingConfProvider

import io.tolgee.dtos.response.PublicBillingConfigurationDTO

interface PublicBillingConfProvider {
  operator fun invoke(): PublicBillingConfigurationDTO
}
