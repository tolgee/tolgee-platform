package io.tolgee.events

import io.tolgee.model.Organization

class OnOrganizationNameUpdated(
  val oldName: String,
  val organization: Organization,
)
