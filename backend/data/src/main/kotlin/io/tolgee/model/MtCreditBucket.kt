package io.tolgee.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault
import java.util.Date

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["organization_id"], name = "mt_credit_bucket_organization_unique"),
  ],
)
class MtCreditBucket(
  @OneToOne(fetch = FetchType.LAZY)
  @Deprecated("Only organization can own a credit bucket...")
  var userAccount: UserAccount? = null,
  @OneToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null,
) : StandardAuditModel() {
  var credits: Long = 0

  /**
   * These credits are not refilled or reset every period.
   * It's consumed when user is out of their standard credits.
   *
   * (In Tolgee Cloud users can buy these Extra credits)
   *
   */
  @ColumnDefault("0")
  @Deprecated(
    "Extra credits should not be used anymore, we only keep them to " +
      "track who purchased them before. Will be removed in future releases when there cases are handled.",
  )
  var extraCredits: Long = 0

  var bucketSize: Long = 0

  var refilled: Date = Date()
}
