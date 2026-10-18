package io.tolgee.service.organization

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.OrganizationFeatureGrantsDto
import io.tolgee.repository.OrganizationFeatureGrantRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Suppress("SelfReferenceConstructorParameter")
class OrganizationFeatureGrantService(
  private val organizationFeatureGrantRepository: OrganizationFeatureGrantRepository,
  @Lazy
  private val self: OrganizationFeatureGrantService,
  private val publicBillingConfProvider: PublicBillingConfProvider,
  private val internalProperties: InternalProperties,
) {
  fun withGrants(
    organizationId: Long?,
    planFeatures: Array<Feature>,
  ): Array<Feature> {
    val granted = getEffectiveGrantedFeatures(organizationId)
    if (granted.isEmpty()) {
      return planFeatures
    }
    return (planFeatures.toSet() + granted).toTypedArray()
  }

  fun getEffectiveGrantedFeatures(organizationId: Long?): Set<Feature> {
    if (organizationId == null || !arePlanFeatureGrantsAllowed()) {
      return emptySet()
    }
    return self.getStoredGrants(organizationId).features
  }

  fun arePlanFeatureGrantsAllowed(): Boolean =
    publicBillingConfProvider().enabled || internalProperties.allowPlanFeatureGrants

  @Cacheable(Caches.ORGANIZATION_FEATURE_GRANTS, key = "#organizationId")
  @Transactional(readOnly = true)
  fun getStoredGrants(organizationId: Long): OrganizationFeatureGrantsDto {
    val grants = organizationFeatureGrantRepository.findAllByOrganizationId(organizationId)
    return OrganizationFeatureGrantsDto(
      features = grants.map { it.feature }.toSet(),
    )
  }
}
