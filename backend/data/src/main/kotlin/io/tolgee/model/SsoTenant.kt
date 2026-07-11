package io.tolgee.model

import io.tolgee.api.ISsoTenant
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(name = "tenant")
class SsoTenant :
  StandardAuditModel(),
  ISsoTenant {
  /**
   * The domain column uses unique constraint.
   * When the tenant is enabled the domain must not be empty and must be unique across all enabled tenants.
   */
  override var domain: String = ""
  override var clientId: String = ""
  override var clientSecret: String = ""
  override var authorizationUri: String = ""
  override var tokenUri: String = ""

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  lateinit var organization: Organization

  override val global: Boolean
    get() = false

  @ColumnDefault("false")
  override var force: Boolean = false

  @ColumnDefault("true")
  var enabled: Boolean = true
}
