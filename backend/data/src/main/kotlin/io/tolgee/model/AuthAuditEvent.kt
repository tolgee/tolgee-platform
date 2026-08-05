package io.tolgee.model

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.enums.AuthAuditEventType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Type

/**
 * Append-only authentication audit record. Deliberately without foreign keys - the trail has to
 * survive deletion of the accounts it describes.
 */
@Entity
@Table(
  name = "auth_audit_event",
  indexes = [
    Index(name = "auth_audit_event_created_at", columnList = "created_at"),
    Index(name = "auth_audit_event_user_account_id", columnList = "user_account_id"),
  ],
)
class AuthAuditEvent : StandardAuditModel() {
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  lateinit var type: AuthAuditEventType

  @Column(name = "user_account_id")
  var userAccountId: Long? = null

  /**
   * The identity the request tried to authenticate as - the only handle on a failed login for a
   * username that matches no account.
   */
  @Column(name = "attempted_username", length = 255)
  var attemptedUsername: String? = null

  @Column(name = "acting_user_account_id")
  var actingUserAccountId: Long? = null

  @Column(name = "device_id", length = 36)
  var deviceId: String? = null

  /**
   * Id of the object the event is about (session, API key, PAT).
   */
  @Column(name = "target_id")
  var targetId: Long? = null

  @Column(length = 64)
  var ip: String? = null

  @Column(name = "user_agent", length = 255)
  var userAgent: String? = null

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var data: MutableMap<String, Any?>? = null
}
