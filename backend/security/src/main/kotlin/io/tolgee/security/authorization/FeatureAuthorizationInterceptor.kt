package io.tolgee.security.authorization

import io.tolgee.security.OrganizationHolder
import io.tolgee.service.featureFlags.FeatureFlagService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
  private val organizationFeatureGuard: OrganizationFeatureGuard,
  private val organizationHolder: OrganizationHolder,
  private val featureFlagService: FeatureFlagService,
) : AbstractAuthorizationInterceptor(allowGlobalRoutes = false) {
  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    checkFeatureFlags(handler)

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

    val orgId = organizationHolder.organization.id

    if (requiresFeatures != null) {
      organizationFeatureGuard.checkFeaturesEnabled(orgId, requiresFeatures.features)
    }

    if (requiresOneOfFeatures != null) {
      organizationFeatureGuard.checkOneOfFeaturesEnabled(orgId, requiresOneOfFeatures.features)
    }

    return true
  }

  private fun checkFeatureFlags(handler: HandlerMethod) {
    val requiresFeatureFlag =
      AnnotationUtils.findAnnotation(handler.method, RequiresFeatureFlag::class.java)
        ?: AnnotationUtils.findAnnotation(handler.beanType, RequiresFeatureFlag::class.java)
        ?: return

    val orgId = organizationHolder.organization.id
    requiresFeatureFlag.flags.forEach { featureFlagService.checkEnabled(orgId, it) }
  }
}
