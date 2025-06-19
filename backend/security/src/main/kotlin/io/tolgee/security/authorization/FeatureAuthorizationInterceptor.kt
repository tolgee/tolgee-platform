package io.tolgee.security.authorization

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.security.OrganizationHolder
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * This interceptor checks if the required features are enabled for the organization.
 * It should run after OrganizationAuthorizationInterceptor and ProjectAuthorizationInterceptor
 * as they are responsible for populating the organizationHolder.
 */
@Component
class FeatureAuthorizationInterceptor(
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val organizationHolder: OrganizationHolder,
) : AbstractAuthorizationInterceptor() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    val requiresFeatures = AnnotationUtils.getAnnotation(handler.method, RequiresFeatures::class.java)
    val requiresOneOfFeatures = AnnotationUtils.getAnnotation(handler.method, RequiresOneOfFeatures::class.java)

    if (requiresFeatures == null && requiresOneOfFeatures == null) {
      // No feature requirements, continue
      return true
    }

    if (requiresFeatures != null && requiresOneOfFeatures != null) {
      // Policy doesn't make sense
      throw RuntimeException(
        "Both `@RequiresFeatures` and `@RequiresOneOfFeatures` have been set for this endpoint!",
      )
    }

    if (requiresFeatures != null) {
      checkFeaturesEnabled(organizationHolder.organization.id, requiresFeatures.features.toList())
    }

    if (requiresOneOfFeatures != null) {
      checkOneOfFeaturesEnabled(organizationHolder.organization.id, requiresOneOfFeatures.features.toList())
    }

    return true
  }

  private fun checkFeaturesEnabled(
    organizationId: Long,
    features: List<Feature>,
  ) {
    for (feature in features) {
      if (!enabledFeaturesProvider.isFeatureEnabled(organizationId, feature)) {
        logger.debug(
          "Feature {} is not enabled for org#{}",
          feature,
          organizationId,
        )
        throw BadRequestException(Message.FEATURE_NOT_ENABLED, listOf(feature))
      }
    }
  }

  private fun checkOneOfFeaturesEnabled(
    organizationId: Long,
    features: List<Feature>,
  ) {
    if (features.find { enabledFeaturesProvider.isFeatureEnabled(organizationId, it) } == null) {
      logger.debug(
        "None of the features {} are enabled for org#{}",
        features,
        organizationId,
      )
      throw BadRequestException(Message.FEATURE_NOT_ENABLED, features)
    }
  }
}
