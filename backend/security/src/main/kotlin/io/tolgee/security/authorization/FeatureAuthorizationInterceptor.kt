package io.tolgee.security.authorization

import io.tolgee.security.OrganizationHolder
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
  private val featureCheckService: FeatureCheckService,
  private val organizationHolder: OrganizationHolder,
) : AbstractAuthorizationInterceptor(allowGlobalRoutes = false) {
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

    val orgId = organizationHolder.organization.id

    if (requiresFeatures != null) {
      featureCheckService.checkFeaturesEnabled(orgId, requiresFeatures.features)
    }

    if (requiresOneOfFeatures != null) {
      featureCheckService.checkOneOfFeaturesEnabled(orgId, requiresOneOfFeatures.features)
    }

    return true
  }
}
