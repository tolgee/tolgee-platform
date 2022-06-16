package io.tolgee.model

import org.hibernate.annotations.ColumnDefault
import java.util.*
import javax.persistence.Entity
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["organization_id"], name = "mt_credit_bucket_organization_unique"),
  ]
)
class MtCreditBucket(
  @OneToOne
  @Deprecated("Only organization can own a credit bucket...")
  var userAccount: UserAccount? = null,

  @OneToOne
  var organization: Organization? = null
) : StandardAuditModel() {

  var credits: Long = 0

  /**
   * These credits are not refilled or reset every period.
   * It's consumed when user is out of their standard credits.
   *
   * (In Tolgee Cloud users can buy these Extra credits)
   */
  @ColumnDefault("0")
  var extraCredits: Long = 0

  var bucketSize: Long = 0

  var refilled: Date = Date()
}
