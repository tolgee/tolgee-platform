package io.tolgee.model.slackIntegration

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["slack_team_id"],
      name = "organization_slack_workspace_slack_team_id_unique",
    ),
  ],
  indexes = [
    Index(columnList = "organization_id"),
    Index(columnList = "author_id"),
  ],
)
class OrganizationSlackWorkspace : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var organization: Organization

  @ManyToOne
  lateinit var author: UserAccount

  lateinit var slackTeamId: String

  lateinit var slackTeamName: String

  lateinit var accessToken: String

  @OneToMany(mappedBy = "organizationSlackWorkspace", fetch = FetchType.LAZY, orphanRemoval = true)
  var slackSubscriptions: MutableList<SlackConfig> = mutableListOf()
}
