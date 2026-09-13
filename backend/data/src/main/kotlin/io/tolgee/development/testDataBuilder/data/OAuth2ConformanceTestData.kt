package io.tolgee.development.testDataBuilder.data

/**
 * Shared by the RFC conformance suites: the default user drives the flow and [otherUser] stands in for a second
 * account, so a grant can be shown not to answer for anyone but the user who consented to it.
 */
class OAuth2ConformanceTestData : BaseTestData() {
  val otherUser = root.addUserAccount { username = "oauth_conformance_other" }.self
}
