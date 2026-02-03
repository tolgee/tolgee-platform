package io.tolgee.events

import io.tolgee.model.Organization

open class BeforeOrganizationDeleteEvent(
  val organization: Organization,
)
