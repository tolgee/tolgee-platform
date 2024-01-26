package io.tolgee.component.automations.processors.slackIntegration

open class SlackException: Exception()

class SlackNotConnectedException(val slackChannelId: String)
  : SlackException()
