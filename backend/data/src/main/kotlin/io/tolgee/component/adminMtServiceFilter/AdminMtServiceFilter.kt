package io.tolgee.component.adminMtServiceFilter

interface AdminMtServiceFilter {
  fun shouldSkipOrgLlmProviders(organizationId: Long): Boolean
}
