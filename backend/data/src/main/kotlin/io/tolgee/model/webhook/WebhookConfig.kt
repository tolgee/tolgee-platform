package io.tolgee.model.webhook

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityIgnoredProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.component.automations.processors.WebhookEventType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import java.util.Date
import org.hibernate.annotations.Type

@Entity
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
class WebhookConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel() {
  @NotBlank
  @ActivityLoggedProp
  @ActivityDescribingProp
  var url: String = ""

  @NotBlank
  var webhookSecret: String = ""

  @ActivityLoggedProp
  var enabled: Boolean = true

  @ActivityLoggedProp
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var eventTypes: MutableSet<WebhookEventType> = mutableSetOf(WebhookEventType.PROJECT_ACTIVITY)

  @OneToMany(mappedBy = "webhookConfig", orphanRemoval = true)
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  @ActivityIgnoredProp
  var firstFailed: Date? = null

  @ActivityIgnoredProp
  var lastExecuted: Date? = null

  @ActivityIgnoredProp
  var autoDisableNotified: Boolean = false

  @ActivityIgnoredProp
  var autoDisabled: Boolean = false
}
