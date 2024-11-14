package io.tolgee.model

import io.tolgee.api.ISsoTenant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(name = "tenant")
class SsoTenant : ISsoTenant, StandardAuditModel() {
  override var name: String = ""
  override var clientId: String = ""
  override var clientSecret: String = ""
  override var authorizationUri: String = ""

  @Column(unique = true, nullable = false)
  @NotBlank
  override var domain: String = ""
  override var jwtSetUri: String = ""
  override var tokenUri: String = ""

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  override lateinit var organization: Organization

  override val global: Boolean
    get() = false

  @ColumnDefault("true")
  var enabled: Boolean = true
}
