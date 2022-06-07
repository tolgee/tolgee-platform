package io.tolgee.component.publicBillingConfProvider

import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import org.springframework.stereotype.Component

@Component
class BasePublicBillingConfProvider : PublicBillingConfProvider {
  override operator fun invoke(): PublicBillingConfigurationDTO {
    return PublicBillingConfigurationDTO(false)
  }
}
