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
 * What this server knows about reading one `client_id`'s metadata document: when it last tried, when it tried before
 * that, and since when the tries have been failing. One row per client, not per grant, because every fact here is
 * about the document and not about any one user's authorization.
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

  /** The latest attempt, written before the document is fetched so that only one caller fetches per interval. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  var checkedAt: Date = Date()

  /**
   * The attempt before [checkedAt]. A withdrawal mark written after this moment has not yet survived a read, so it
   * may still be a mis-deploy. A wall clock cannot answer that on its own: a client nobody refreshes is not read at
   * all, however much time passes.
   */
  @Temporal(TemporalType.TIMESTAMP)
  var previousCheckedAt: Date? = null

  /**
   * The first failed attempt since the document was last read successfully. Null while the document reads fine.
   * Counting from here, and not from the last success, is what keeps a single blip after a long idle period from
   * ending a grant.
   */
  @Temporal(TemporalType.TIMESTAMP)
  var failingSince: Date? = null
}
