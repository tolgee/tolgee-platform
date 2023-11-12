package io.tolgee.model.webhook

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.validation.constraints.NotBlank

@Entity
class WebhookConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel() {
  @NotBlank
  var url: String = ""

  @NotBlank
  var webhookSecret: String = ""
}
