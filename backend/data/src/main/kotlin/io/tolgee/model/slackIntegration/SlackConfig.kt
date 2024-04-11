package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type

@Entity
class SlackConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount,
  var channelId: String,
) : StandardAuditModel() {
  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true, fetch = FetchType.LAZY)
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var languageTags: MutableSet<String> = hashSetOf()

  @Enumerated(EnumType.STRING)
  var onEvent: EventName = EventName.ALL

  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true, fetch = FetchType.LAZY)
  @Column(nullable = true)
  var savedSlackMessage: MutableList<SavedSlackMessage> = mutableListOf()

  @ColumnDefault("false")
  @Column(nullable = true)
  var isGlobalSubscription: Boolean = false

  @OneToMany(mappedBy = "slackConfig", fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], orphanRemoval = true)
  var preferences: MutableSet<SlackConfigPreference> = mutableSetOf()

  @ManyToOne(fetch = FetchType.LAZY)
  var organizationSlackWorkspace: OrganizationSlackWorkspace = OrganizationSlackWorkspace()
}
