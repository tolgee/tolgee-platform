package io.tolgee.service.apps

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.model.apps.App
import org.springframework.stereotype.Component

/**
 * Carries the acting app onto the request's activity revision: the id and name ride the
 * activity→business-event bridge to PostHog (see [io.tolgee.activity.ActivityHolder.businessEventData]),
 * and [organizationId] pre-seeds the revision for paths that have no `OrganizationHolder` (app-auth
 * and server-admin), where [io.tolgee.activity.iterceptor.ActivityRevisionInitializer] preserves the
 * value already set. Must run before the operation's transaction commits, since the bridge reads the
 * data when the revision is stored.
 */
@Component
class AppActivityRecorder(
  private val activityHolder: ActivityHolder,
) {
  fun record(
    app: App,
    activityType: ActivityType? = null,
    organizationId: Long? = null,
    projectId: Long? = null,
  ) {
    activityType?.let { activityHolder.activity = it }
    activityHolder.businessEventData["appId"] = app.id.toString()
    activityHolder.businessEventData["appName"] = app.name
    organizationId?.let { activityHolder.activityRevision.organizationId = it }
    projectId?.let { activityHolder.activityRevision.projectId = it }
  }
}
