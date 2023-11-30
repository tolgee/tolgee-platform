package io.tolgee.model.webhook

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.automations.AutomationAction
import java.util.*
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.validation.constraints.NotBlank

@Entity
class WebhookConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel() {
  @NotBlank
  var url: String = ""

  @NotBlank
  var webhookSecret: String = ""

  @OneToMany(mappedBy = "webhookConfig", orphanRemoval = true)
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  var firstFailed: Date? = null

  var lastExecuted: Date? = null
}
