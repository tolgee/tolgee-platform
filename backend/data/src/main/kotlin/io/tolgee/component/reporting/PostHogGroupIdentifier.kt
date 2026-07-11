package io.tolgee.component.reporting

interface PostHogGroupIdentifier {
  fun identifyOrganization(organizationId: Long)

  companion object {
    const val GROUP_TYPE = "organization"
  }
}
