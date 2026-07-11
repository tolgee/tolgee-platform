package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import io.tolgee.model.UserAccount
import io.tolgee.model.UserAccount.AccountType
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ThirdPartyAuthType
import java.util.Date
import java.util.UUID

class AuthProviderChangeEeTestData(
  currentDate: Date,
) : AuthProviderChangeTestData(currentDate) {
  var userNoProviderForcedSsoOrganization: UserAccount
  var userSsoGlobal: UserAccount
  var userSsoOrganizations: UserAccount
  var userChangeNoneToSsoGlobal: UserAccount
  var userChangeNoneToSsoOrganizations: UserAccount
  var userChangeGoogleToSsoGlobal: UserAccount
  var userChangeOauth2ToSsoOrganizations: UserAccount

  lateinit var changeNoneToSsoGlobal: AuthProviderChangeRequest
  lateinit var changeNoneToSsoOrganizations: AuthProviderChangeRequest
  lateinit var changeGoogleToSsoGlobal: AuthProviderChangeRequest
  lateinit var changeOauth2ToSsoOrganizations: AuthProviderChangeRequest

  var organization: Organization
  var organizationForced: Organization
  var tenant: SsoTenant
  lateinit var tenantForced: SsoTenant

  init {
    organization = userAccountBuilder.defaultOrganizationBuilder.self
    tenant =
      userAccountBuilder.defaultOrganizationBuilder
        .setTenant {
          enabled = true
          domain = "domain-org.com"
          clientId = "dummy_client_id"
          clientSecret = "clientSecret"
          authorizationUri = "https://dummy-url.com"
          tokenUri = "http://tokenUri"
        }.self

    organizationForced =
      root
        .addOrganization {
          name = "organizationForced"
        }.build {
          tenantForced =
            setTenant {
              enabled = true
              force = true
              domain = "org-forced.com"
              clientId = "dummy_client_id"
              clientSecret = "clientSecret"
              authorizationUri = "https://dummy-url.com"
              tokenUri = "http://tokenUri"
            }.self
        }.self

    userNoProviderForcedSsoOrganization =
      root
        .addUserAccount {
          username = "userNoProviderForcedSsoOrganization@org-forced.com"
        }.self

    userSsoGlobal =
      root
        .addUserAccount {
          username = "userSsoGlobal@domain.com"
          accountType = AccountType.MANAGED
          thirdPartyAuthType = ThirdPartyAuthType.SSO_GLOBAL
          thirdPartyAuthId = "aaa6"
          ssoRefreshToken = "bbb"
          ssoSessionExpiry = validExpirationDate
        }.self
    userSsoOrganizations =
      root
        .addUserAccount {
          username = "userSsoOrganizations@domain-org.com"
          accountType = AccountType.MANAGED
          thirdPartyAuthType = ThirdPartyAuthType.SSO
          thirdPartyAuthId = "aaa7"
          ssoSessionExpiry = validExpirationDate
        }.self
    userAccountBuilder.defaultOrganizationBuilder.build {
      addRole {
        user = userSsoOrganizations
        type = OrganizationRoleType.MEMBER
        managed = true
      }
    }

    userChangeNoneToSsoGlobal =
      root
        .addUserAccount {
          username = "userChangeNoneToSsoGlobal@domain.com"
        }.build {
          changeNoneToSsoGlobal =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = ThirdPartyAuthType.SSO_GLOBAL
              authId = "aaa8"

              ssoDomain = "domain.com"
              ssoRefreshToken = "bbb"
              expirationDate = validExpirationDate
              ssoExpiration = validExpirationDate
            }.self
        }.self
    userChangeNoneToSsoOrganizations =
      root
        .addUserAccount {
          username = "userChangeNoneToSsoOrganizations@domain-org.com"
        }.build {
          changeNoneToSsoOrganizations =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = ThirdPartyAuthType.SSO
              authId = "aaa9"

              ssoDomain = "domain-org.com"
              ssoRefreshToken = "bbb"
              expirationDate = validExpirationDate
              ssoExpiration = validExpirationDate
            }.self
        }.self

    userChangeGoogleToSsoGlobal =
      root
        .addUserAccount {
          username = "userChangeGoogleToSsoGlobal@domain.com"
          thirdPartyAuthType = ThirdPartyAuthType.GOOGLE
          thirdPartyAuthId = "aaa10"
        }.build {
          changeGoogleToSsoGlobal =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = ThirdPartyAuthType.SSO_GLOBAL
              authId = "aaa11"

              ssoDomain = "domain.com"
              ssoRefreshToken = "bbb"
              expirationDate = validExpirationDate
              ssoExpiration = validExpirationDate
            }.self
        }.self
    userChangeOauth2ToSsoOrganizations =
      root
        .addUserAccount {
          username = "userChangeOauth2ToSsoOrganizations@domain-org.com"
          accountType = AccountType.THIRD_PARTY
          thirdPartyAuthType = ThirdPartyAuthType.OAUTH2
          thirdPartyAuthId = "aaa12"
        }.build {
          rawPassword = null
          changeOauth2ToSsoOrganizations =
            setAuthProviderChangeRequest {
              identifier = UUID.randomUUID().toString()
              authType = ThirdPartyAuthType.SSO
              authId = "aaa13"

              ssoDomain = "domain-org.com"
              ssoRefreshToken = "bbb"
              expirationDate = validExpirationDate
              ssoExpiration = validExpirationDate
            }.self
        }.self
  }
}
