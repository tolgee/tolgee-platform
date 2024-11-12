package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "user_account_id"),
    Index(columnList = "organization_slack_workspace_id"),
  ],
)
class SlackConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount,
  var channelId: String = "",
) : StandardAuditModel() {
  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true, fetch = FetchType.LAZY)
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var languageTags: MutableSet<String> = hashSetOf()

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var events: MutableSet<SlackEventType> = mutableSetOf()

  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true, fetch = FetchType.LAZY)
  @Column(nullable = true)
  var savedSlackMessage: MutableList<SavedSlackMessage> = mutableListOf()

  @ColumnDefault("false")
  @Column(nullable = true)
  var isGlobalSubscription: Boolean = false

  @OneToMany(mappedBy = "slackConfig", fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], orphanRemoval = true)
  var preferences: MutableSet<SlackConfigPreference> = mutableSetOf()

  @ManyToOne(fetch = FetchType.LAZY)
  var organizationSlackWorkspace: OrganizationSlackWorkspace? = null
}
