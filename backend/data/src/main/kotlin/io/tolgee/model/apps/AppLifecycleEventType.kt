package io.tolgee.model.apps

/**
 * What a lifecycle delivery tells the app. The wire value is [wireName] — an app matches on it, so
 * it may not follow the enum constant if that is ever renamed.
 */
enum class AppLifecycleEventType(
  val wireName: String,
) {
  APP_REGISTERED("app.registered"),
  APP_INSTALLED("app.installed"),
  APP_UNINSTALLED("app.uninstalled"),
  APP_SECRET_ROTATED("app.secret_rotated"),
}
