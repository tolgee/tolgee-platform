package io.tolgee.activity.iterceptor

import io.tolgee.activity.ActivityHolder
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.OrganizationNotSelectedException
import io.tolgee.security.ProjectHolder
import io.tolgee.security.ProjectNotSelectedException
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationContext

/**
 * Helper class responsible for populating ActivityRevision entity with base properties.
 * It works in conjunction with InterceptedEventsManager to set up revision properties
 * like author, organization, project and activity type.
 */
class ActivityRevisionInitializer(
  private val applicationContext: ApplicationContext,
  private val revision: ActivityRevision,
  private val activityHolder: ActivityHolder,
) : Logging {
  fun initialize() {
    revision.isInitializedByInterceptor = true
    revision.authorId = userAccount?.id ?: revision.authorId
    revision.organizationId = organizationId ?: revision.organizationId
    revision.projectId = project?.id ?: revision.projectId
    revision.type = activityHolder.activity ?: revision.type
  }

  /**
   * Lazily retrieves a project from ProjectHolder.
   * Returns null if a project is not selected
   */
  private val project: ProjectDto? by lazy {
    try {
      projectHolder.project
    } catch (e: ProjectNotSelectedException) {
      logger.debug("Project is not set in ProjectHolder. Activity will be stored without projectId.", e)
      null
    }
  }

  /**
   * Tries to get organization id from the organization holder.
   *
   * If by any chance it's not populated, we look into the projectHolder and try to get the
   * organizationOwnerId from the ProjectDto
   */
  private val organizationId: Long? by lazy {
    try {
      organizationHolder.organization.id
    } catch (e: OrganizationNotSelectedException) {
      project?.organizationOwnerId ?: let {
        logger.debug(
          "Organization is not set in OrganizationHolder. Activity will be stored without organizationId.",
          e,
        )
        null
      }
    }
  }

  private val projectHolder: ProjectHolder by lazy {
    applicationContext.getBean(ProjectHolder::class.java)
  }

  private val organizationHolder: OrganizationHolder by lazy {
    applicationContext.getBean(OrganizationHolder::class.java)
  }

  private val userAccount: UserAccountDto?
    get() = authenticationFacade.authenticatedUserOrNull

  private val authenticationFacade: AuthenticationFacade by lazy {
    applicationContext.getBean(AuthenticationFacade::class.java)
  }
}
