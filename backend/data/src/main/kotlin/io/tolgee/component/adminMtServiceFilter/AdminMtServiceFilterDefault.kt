package io.tolgee.component.adminMtServiceFilter

import org.springframework.stereotype.Component

@Component
class AdminMtServiceFilterDefault : AdminMtServiceFilter {
  override fun shouldSkipOrgLlmProviders(organizationId: Long) = false
}
