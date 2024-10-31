package io.tolgee.ee.model

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(schema = "ee", name = "tenant")
class SsoTenant : StandardAuditModel() {
  var name: String = ""
  var ssoProvider: String = ""
  var clientId: String = ""
  var clientSecret: String = ""
  var authorizationUri: String = ""
  var domain: String = ""
  var jwkSetUri: String = ""
  var tokenUri: String = ""
  var organizationId: Long = 0L

  @ColumnDefault("true")
  var isEnabledForThisOrganization: Boolean = true
}
