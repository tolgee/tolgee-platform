package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.AppBuilder
import io.tolgee.model.Project
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

  /**
   * An app [otherOrganization] owns and made available to the owner organization only (no
   * all-organizations sentinel), installed by the owner organization and enabled for its project.
   * Withdrawing the single availability grant makes the app unreachable for the owner org, so its
   * enablement rows are deleted - the scenario the availability-withdrawal eviction test drives.
   */
  lateinit var orgScopedApp: App
  lateinit var orgScopedInstall: AppInstall
  lateinit var orgScopedProject: Project

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
          manifestJson = AppBuilder.manifestJsonFor("available-app", "Available App")
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

      val orgScopedAppBuilder =
        otherOrganizationBuilder.addApp {
          appId = "org-scoped-app"
          name = "Org Scoped App"
          manifestJson = AppBuilder.manifestJsonFor("org-scoped-app", "Org Scoped App")
        }
      orgScopedApp = orgScopedAppBuilder.self
      orgScopedAppBuilder.addAvailability { organization = ownerOrgBuilder.self }

      orgScopedInstall =
        ownerOrgBuilder
          .addAppInstall {
            app = this@AppsWithInstallsTestData.orgScopedApp
          }.self

      orgScopedProject = projectBuilder.self
      projectBuilder.addEnabledApp { appInstall = this@AppsWithInstallsTestData.orgScopedInstall }
    }
  }
}
