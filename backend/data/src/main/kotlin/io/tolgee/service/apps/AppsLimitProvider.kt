package io.tolgee.service.apps

/**
 * How many apps this server (or, on cloud, the organization's plan) allows. The community edition
 * enforces its limit here in the open-source core deliberately — removing the ee content does not
 * lift it. -1 means unlimited.
 */
interface AppsLimitProvider {
  fun getAppsLimit(organizationId: Long): Long
}
