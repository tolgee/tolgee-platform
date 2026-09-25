package io.tolgee.model.oauth2

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * A refresh token a grant rotated away more than one generation ago — the grant itself remembers only its current and
 * immediately-previous one, so without this a replay from further back fails generically instead of as theft.
 */
@Entity
@Table(
  name = "oauth2_superseded_refresh_token",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["token_hash"], name = "oauth2_superseded_refresh_token_hash_unique"),
  ],
  indexes = [Index(columnList = "grant_id, superseded_at DESC")],
)
class OAuth2SupersededRefreshToken : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var grant: OAuth2Grant

  @Column(nullable = false)
  var tokenHash: String = ""

  @Temporal(TemporalType.TIMESTAMP)
  var supersededAt: Date? = null
}
