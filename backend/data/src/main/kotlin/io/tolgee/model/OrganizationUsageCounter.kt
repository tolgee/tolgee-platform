package io.tolgee.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.util.Date

@Entity
@Table(name = "organization_usage_counter")
class OrganizationUsageCounter(
  @OneToOne
  @MapsId
  @JoinColumn(name = "organization_id")
  var organization: Organization,
) : AuditModel() {
  @Id
  @Column(name = "organization_id")
  var organizationId: Long = 0

  @Column(name = "key_count", nullable = false)
  var keyCount: Long = 0

  @Column(name = "translation_count", nullable = false)
  var translationCount: Long = 0

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_reconciled_at")
  var lastReconciledAt: Date? = null
}
