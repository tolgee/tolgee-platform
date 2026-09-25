package io.tolgee.model.oauth2

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * When a `client_id`'s metadata document was last *attempted*, which is what orders the background check's work
 * list. One row per client, not per grant. `OAuth2Grant.cimdVerifiedAt` records the last *successful* read instead.
 */
@Entity
@Table(
  name = "oauth2_client_document_check",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["client_id"], name = "oauth2_client_document_check_client_id_unique"),
  ],
)
class OAuth2ClientDocumentCheck : StandardAuditModel() {
  @Column(nullable = false)
  var clientId: String = ""

  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  var checkedAt: Date = Date()

  /**
   * The attempt before [checkedAt]. A withdrawal mark written after this moment has not yet survived a read, so it
   * may still be a mis-deploy. A wall clock cannot answer that: how soon a client is read again is a queue
   * position, not a duration.
   */
  @Temporal(TemporalType.TIMESTAMP)
  var previousCheckedAt: Date? = null
}
