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
class SsoTenant : ISsoTenant, StandardAuditModel() {
  override var clientId: String = ""
  override var clientSecret: String = ""
  override var authorizationUri: String = ""

  /**
   * The domain column uses unique constraint.
   * When the tenant is enabled the domain must not be empty and must be unique across all enabled tenants.
   */
  override var domain: String = ""

//  override var jwkSetUri: String = ""
  override var tokenUri: String = ""

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  lateinit var organization: Organization

  override val global: Boolean
    get() = false

  @ColumnDefault("true")
  var enabled: Boolean = true
}
