package io.tolgee.ee.model

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(schema = "ee")
class Tenant : StandardAuditModel() {
  var name: String = ""
  var ssoProvider: String = ""
  var clientId: String = ""
  var clientSecret: String = ""
  var authorizationUri: String = ""
  var domain: String = ""
  var jwkSetUri: String = ""
  var tokenUri: String = ""
  var redirectUriBase: String = "" // base Tolgee frontend url can be different for different users so need to store it
}
