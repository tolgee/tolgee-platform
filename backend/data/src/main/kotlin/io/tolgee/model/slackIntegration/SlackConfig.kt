package io.tolgee.model.slackIntegration

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault

@Entity
class SlackConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount,
  var channelId: String,
): StandardAuditModel() {
  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true, fetch = FetchType.LAZY)
  var automationActions: MutableList<AutomationAction> = mutableListOf()
  @Column(nullable = false)
  var slackId: String = ""
  @ElementCollection
  var languageTags: MutableSet<String> = hashSetOf()
  @ColumnDefault("1")
  var visibilityOptions: VisibilityOptions = VisibilityOptions.ONLY_ME
  @ColumnDefault("0")
  var onEvent: EventName = EventName.ALL
  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true, fetch = FetchType.LAZY)
  @Column(nullable = true)
  var savedSlackMessage: MutableList<SavedSlackMessage> = mutableListOf()
  @ColumnDefault("false")
  @Column(nullable = true)
  var isGlobalSubscription: Boolean = false
}
