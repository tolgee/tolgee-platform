package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.enums.Scope

/**
 * The app graph declared through the test-data DSL: a registered app installed by its owner and by
 * another organization, and a second app made available (to all and to one organization) and
 * enabled for the owner's project. Consumed by the administration apps tests so no app has to be
 * registered or installed through the service layer or over HTTP.
 */
class AppsWithInstallsTestData : NativeAppsTestData() {
  lateinit var app: App
  lateinit var ownerInstall: AppInstall
  lateinit var otherOrgInstall: AppInstall

  lateinit var availableApp: App
  lateinit var enabledInstall: AppInstall

  init {
    root.apply {
      val ownerOrgBuilder = userAccountBuilder.defaultOrganizationBuilder

      val appBuilder =
        ownerOrgBuilder.addApp {
          appId = "test-app"
          name = "Test App"
          manifestScopes = "keys.edit,translations.view"
        }
      app = appBuilder.self
      appBuilder.addSecret { }

      ownerInstall =
        ownerOrgBuilder
          .addAppInstall {
            app = this@AppsWithInstallsTestData.app
            grantedScopes = arrayOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT)
          }.self

      otherOrgInstall =
        otherOrganizationBuilder
          .addAppInstall {
            app = this@AppsWithInstallsTestData.app
            grantedScopes = arrayOf(Scope.TRANSLATIONS_VIEW)
          }.self

      val availableAppBuilder =
        ownerOrgBuilder.addApp {
          appId = "available-app"
          name = "Available App"
        }
      availableApp = availableAppBuilder.self
      availableAppBuilder.addAvailableToAll()
      availableAppBuilder.addAvailability { organization = this@AppsWithInstallsTestData.otherOrganization }

      enabledInstall =
        ownerOrgBuilder
          .addAppInstall {
            app = this@AppsWithInstallsTestData.availableApp
          }.self
      projectBuilder.addEnabledApp { appInstall = this@AppsWithInstallsTestData.enabledInstall }
    }
  }
}
