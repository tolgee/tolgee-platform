package io.tolgee.service.organization

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationFeatureGrant
import io.tolgee.model.UserAccount
import io.tolgee.repository.OrganizationFeatureGrantRepository
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrganizationFeatureGrantChangeService(
  private val organizationFeatureGrantRepository: OrganizationFeatureGrantRepository,
  private val organizationFeatureGrantService: OrganizationFeatureGrantService,
  private val entityManager: EntityManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @Transactional
  @CacheEvict(Caches.ORGANIZATION_FEATURE_GRANTS, key = "#organizationId")
  fun setGrants(
    organizationId: Long,
    grantedFeatures: Set<Feature>,
    actor: UserAccount?,
  ) {
    if (grantedFeatures.isNotEmpty() && !organizationFeatureGrantService.arePlanFeatureGrantsAllowed()) {
      throw BadRequestException(Message.PLAN_FEATURE_GRANTS_NOT_ALLOWED)
    }
    val previousGrants = replaceStoredGrants(organizationId, grantedFeatures, actor)
    publishEffectiveFeatureChange(organizationId, previousGrants, grantedFeatures)
  }

  private fun replaceStoredGrants(
    organizationId: Long,
    grantedFeatures: Set<Feature>,
    actor: UserAccount?,
  ): Set<Feature> {
    val existing = organizationFeatureGrantRepository.findAllByOrganizationId(organizationId)
    val previousGrants = existing.map { it.feature }.toSet()

    val toDelete = existing.filter { it.feature !in grantedFeatures }
    organizationFeatureGrantRepository.deleteAll(toDelete)

    val organization = entityManager.getReference(Organization::class.java, organizationId)
    val toCreate =
      (grantedFeatures - previousGrants)
        .map { feature ->
          OrganizationFeatureGrant(
            organization = organization,
            feature = feature,
            createdBy = actor,
          )
        }
    organizationFeatureGrantRepository.saveAll(toCreate)
    return previousGrants
  }

  private fun publishEffectiveFeatureChange(
    organizationId: Long,
    previousGrants: Set<Feature>,
    newGrants: Set<Feature>,
  ) {
    if (previousGrants == newGrants) {
      return
    }
    if (!organizationFeatureGrantService.arePlanFeatureGrantsAllowed()) {
      return
    }
    val fromPlan = getFeaturesFromPlan(organizationId)
    val gained = newGrants - previousGrants - fromPlan
    val lost = previousGrants - newGrants - fromPlan
    if (gained.isEmpty() && lost.isEmpty()) {
      return
    }
    applicationEventPublisher.publishEvent(
      OnOrganizationFeaturesChanged(organizationId, gained, lost, grantsAccountedFor = true),
    )
  }

  private fun getFeaturesFromPlan(organizationId: Long): Set<Feature> =
    enabledFeaturesProvider.getPlanFeatures(organizationId).toSet()

  fun getEffectivelyChangedFeatures(
    event: OnOrganizationFeaturesChanged,
    organizationId: Long,
  ): Set<Feature> {
    val changed = event.gainedFeatures + event.lostFeatures
    if (event.grantsAccountedFor) return changed
    return changed - organizationFeatureGrantService.getEffectiveGrantedFeatures(organizationId)
  }
}
