package io.tolgee.dtos.response

import java.math.BigDecimal

class PublicBillingConfigurationDTO(
  val enabled: Boolean,
  val minUsageInvoiceAmount: BigDecimal? = null,
)
