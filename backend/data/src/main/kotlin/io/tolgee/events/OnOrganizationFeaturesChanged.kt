package io.tolgee.events

import io.tolgee.constants.Feature

/**
 * Published when an organization's available features change due to subscription/plan/license changes.
 * @param organizationId The affected org, or null for global feature changes (e.g., self-hosted license).
 */
class OnOrganizationFeaturesChanged(
  val organizationId: Long?,
  val gainedFeatures: Set<Feature>,
  val lostFeatures: Set<Feature>,
)
