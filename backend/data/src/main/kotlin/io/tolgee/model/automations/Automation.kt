package io.tolgee.model.automations

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.validation.constraints.NotBlank

@Entity
class Automation(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel() {

  @NotBlank
  var name: String = ""

  @OneToMany(mappedBy = "automation", orphanRemoval = true)
  var triggers: MutableList<AutomationTrigger> = mutableListOf()

  @OneToMany(mappedBy = "automation", orphanRemoval = true)
  var actions: MutableList<AutomationAction> = mutableListOf()
}
