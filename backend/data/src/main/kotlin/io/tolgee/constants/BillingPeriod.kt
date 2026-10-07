package io.tolgee.constants

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class BillingPeriod {
  @JsonEnumDefaultValue
  MONTHLY,
  YEARLY,
}
