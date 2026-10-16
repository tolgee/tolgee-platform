package io.tolgee.service.featureFlags

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.OrganizationFeatureGrantsDto
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationFeatureGrant
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationFeatureGrantKind
import io.tolgee.repository.OrganizationFeatureGrantRepository
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Suppress("SelfReferenceConstructorParameter")
class OrganizationFeatureGrantService(
  private val organizationFeatureGrantRepository: OrganizationFeatureGrantRepository,
  private val entityManager: EntityManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  @Lazy
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  @Lazy
  private val self: OrganizationFeatureGrantService,
  @Lazy
  private val publicBillingConfProvider: PublicBillingConfProvider,
  private val internalProperties: InternalProperties,
) {
  private val grantsSuppressed = ThreadLocal.withInitial { false }

  @Cacheable(Caches.ORGANIZATION_FEATURE_GRANTS, key = "#organizationId")
  @Transactional(readOnly = true)
  fun getGrants(organizationId: Long): OrganizationFeatureGrantsDto {
    val grants = organizationFeatureGrantRepository.findAllByOrganizationId(organizationId)
    return OrganizationFeatureGrantsDto(
      featureFlags =
        grants
          .filter { it.kind == OrganizationFeatureGrantKind.FEATURE_FLAG }
          .mapNotNull { FeatureFlag.findByName(it.name) }
          .toSet(),
      planFeatures =
        grants
          .filter { it.kind == OrganizationFeatureGrantKind.PLAN_FEATURE }
          .mapNotNull { Feature.findByName(it.name) }
          .toSet(),
    )
  }

  fun arePlanFeatureGrantsAllowed(): Boolean =
    publicBillingConfProvider().enabled || internalProperties.allowPlanFeatureGrants

  fun getGrantedPlanFeatures(organizationId: Long?): Set<Feature> {
    if (organizationId == null || grantsSuppressed.get() || !arePlanFeatureGrantsAllowed()) {
      return emptySet()
    }
    return self.getGrants(organizationId).planFeatures
  }

  fun getEffectivelyChangedFeatures(event: OnOrganizationFeaturesChanged): Set<Feature> {
    val changed = event.gainedFeatures + event.lostFeatures
    if (event.grantsAccountedFor) return changed
    return changed - getGrantedPlanFeatures(event.organizationId)
  }

  fun <T> withoutGrants(fn: () -> T): T {
    val previous = grantsSuppressed.get()
    grantsSuppressed.set(true)
    try {
      return fn()
    } finally {
      grantsSuppressed.set(previous)
    }
  }

  @Transactional
  @CacheEvict(Caches.ORGANIZATION_FEATURE_GRANTS, key = "#organizationId")
  fun setGrants(
    organizationId: Long,
    featureFlags: Set<FeatureFlag>,
    planFeatures: Set<Feature>,
    actor: UserAccount?,
  ) {
    if (planFeatures.isNotEmpty() && !arePlanFeatureGrantsAllowed()) {
      throw BadRequestException(Message.PLAN_FEATURE_GRANTS_NOT_ALLOWED)
    }
    val existing = organizationFeatureGrantRepository.findAllByOrganizationId(organizationId)
    val previousPlanFeatures =
      existing
        .filter { it.kind == OrganizationFeatureGrantKind.PLAN_FEATURE }
        .mapNotNull { Feature.findByName(it.name) }
        .toSet()

    val desired =
      featureFlags.map { OrganizationFeatureGrantKind.FEATURE_FLAG to it.name } +
        planFeatures.map { OrganizationFeatureGrantKind.PLAN_FEATURE to it.name }
    val existingKeys = existing.map { it.kind to it.name }.toSet()

    val toDelete = existing.filter { (it.kind to it.name) !in desired }
    organizationFeatureGrantRepository.deleteAll(toDelete)

    val organization = entityManager.getReference(Organization::class.java, organizationId)
    val toCreate =
      desired
        .filter { it !in existingKeys }
        .map { (kind, name) ->
          OrganizationFeatureGrant(
            organization = organization,
            kind = kind,
            name = name,
            createdBy = actor,
          )
        }
    organizationFeatureGrantRepository.saveAll(toCreate)

    publishFeaturesChanged(organizationId, previousPlanFeatures, planFeatures)
  }

  private fun publishFeaturesChanged(
    organizationId: Long,
    previousGrants: Set<Feature>,
    newGrants: Set<Feature>,
  ) {
    if (previousGrants == newGrants) {
      return
    }
    val fromPlan = withoutGrants { enabledFeaturesProvider.get(organizationId).toSet() }
    val gained = newGrants - previousGrants - fromPlan
    val lost = previousGrants - newGrants - fromPlan
    if (gained.isEmpty() && lost.isEmpty()) {
      return
    }
    applicationEventPublisher.publishEvent(
      OnOrganizationFeaturesChanged(organizationId, gained, lost, grantsAccountedFor = true),
    )
  }
}
