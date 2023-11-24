package io.tolgee.model.automations

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.webhook.WebhookConfig
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class AutomationAction(
  @ManyToOne(fetch = FetchType.LAZY)
  var automation: Automation,
) : StandardAuditModel() {
  var type: AutomationActionType = AutomationActionType.CONTENT_DELIVERY_PUBLISH

  @ManyToOne
  var contentDeliveryConfig: ContentDeliveryConfig? = null

  @ManyToOne
  var webhookConfig: WebhookConfig? = null
}
