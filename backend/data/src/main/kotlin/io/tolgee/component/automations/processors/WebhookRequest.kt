package io.tolgee.component.automations.processors

import io.tolgee.api.IProjectActivityModel

data class WebhookRequest(val webhookConfigId: Long, val data: IProjectActivityModel)
