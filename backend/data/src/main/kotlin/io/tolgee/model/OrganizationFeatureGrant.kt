package io.tolgee.model

import io.tolgee.constants.Feature
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      name = "organization_feature_grant_org_feature",
      columnNames = ["organization_id", "feature"],
    ),
  ],
)
class OrganizationFeatureGrant(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  var organization: Organization,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var feature: Feature,
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  var createdBy: UserAccount? = null,
) : StandardAuditModel()
