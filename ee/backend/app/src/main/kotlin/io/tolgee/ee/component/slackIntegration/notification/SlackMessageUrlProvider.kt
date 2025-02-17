package io.tolgee.ee.component.slackIntegration.notification

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component

@Component
class SlackMessageUrlProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  fun getUrlOnImport(context: SlackMessageContext) =
    "${tolgeeProperties.frontEndUrl}/projects/${context.slackConfig.project.id}/" +
      "activity-detail?activity=${context.activityData?.revisionId}"

  fun getUrlOnSpecifiedKey(
    context: SlackMessageContext,
    keyId: Long,
  ) = "${tolgeeProperties.frontEndUrl}/projects/${context.slackConfig.project.id}/" +
    "translations/single?id=$keyId"
}
