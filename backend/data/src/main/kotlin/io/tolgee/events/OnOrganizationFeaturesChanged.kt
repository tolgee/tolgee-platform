package io.tolgee.events

import io.tolgee.constants.Feature

/**
 * Published when an organization's available features change due to plan, license or grant changes.
 *
 * [gainedFeatures] and [lostFeatures] are raw diffs unless [grantsAccountedFor] is true; resolve
 * them per organization with `OrganizationFeatureGrantChangeService.getEffectivelyChangedFeatures`.
 *
 * @param organizationId The affected org, or null for global feature changes (e.g., self-hosted license).
 */
class OnOrganizationFeaturesChanged(
  val organizationId: Long?,
  val gainedFeatures: Set<Feature>,
  val lostFeatures: Set<Feature>,
  val grantsAccountedFor: Boolean = false,
)
