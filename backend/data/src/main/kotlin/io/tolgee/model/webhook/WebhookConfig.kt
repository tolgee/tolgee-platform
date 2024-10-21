package io.tolgee.model.webhook

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityIgnoredProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import java.util.*

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

  @OneToMany(mappedBy = "webhookConfig", orphanRemoval = true)
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  @ActivityIgnoredProp
  var firstFailed: Date? = null

  @ActivityIgnoredProp
  var lastExecuted: Date? = null
}
