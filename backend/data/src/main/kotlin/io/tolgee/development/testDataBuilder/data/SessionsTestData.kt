package io.tolgee.development.testDataBuilder.data

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.UserSession
import io.tolgee.model.enums.UserSessionType
import io.tolgee.util.addDays
import java.util.Date

class SessionsTestData(
  private val currentDateProvider: CurrentDateProvider,
) {
  lateinit var user: UserAccount
  lateinit var otherUser: UserAccount
  lateinit var unverifiedUser: UserAccount

  lateinit var edgeSession: UserSession
  lateinit var operaSession: UserSession
  lateinit var chromeWindowsSession: UserSession
  lateinit var firefoxSession: UserSession
  lateinit var iphoneSession: UserSession
  lateinit var androidSession: UserSession
  lateinit var chromeMacSession: UserSession
  lateinit var impersonationSession: UserSession
  lateinit var garbageAgentSession: UserSession
  lateinit var firefoxLinuxSession: UserSession
  lateinit var nullAgentSession: UserSession
  lateinit var revokedSession: UserSession
  lateinit var expiredSession: UserSession
  lateinit var otherUserSession: UserSession
  lateinit var unverifiedUserSession: UserSession

  /**
   * `createdAt` is `@CreatedDate`-managed, so builder-set values are overwritten on save. The e2e
   * data controller replays this map with a native update once the rows exist.
   */
  val createdAtByDeviceId = mutableMapOf<String, Date>()

  private val activeExpiry get() = currentDateProvider.date.addDays(30)

  val root =
    TestDataBuilder().apply {
      addUserAccount {
        username = OTHER_USERNAME
        name = "Peter Peter"
        role = UserAccount.Role.USER
        otherUser = this
      }.build {
        addSession {
          type = UserSessionType.LOGIN_NATIVE
          ip = "10.20.0.1"
          countryCode = "DE"
          country = "Germany"
          city = "Berlin"
          userAgent = CHROME_WINDOWS_UA
          lastUsedAt = Date(1661342685000)
          expiresAt = activeExpiry
          otherUserSession = this
          createdAtByDeviceId[deviceId] = Date(1661242685000)
        }
      }

      addUserAccount {
        username = UNVERIFIED_USERNAME
        name = "Ursula Unverified"
        role = UserAccount.Role.USER
        unverifiedUser = this
      }.build {
        addSession {
          type = UserSessionType.SIGN_UP
          ip = "10.30.0.1"
          countryCode = "AT"
          country = "Austria"
          city = "Vienna"
          userAgent = CHROME_WINDOWS_UA
          lastUsedAt = Date(1661342685000)
          expiresAt = activeExpiry
          unverifiedUserSession = this
          createdAtByDeviceId[deviceId] = Date(1661242685000)
        }
      }

      addUserAccount {
        username = USERNAME
        name = "John User"
        role = UserAccount.Role.USER
        user = this
      }.build {
        addSession {
          type = UserSessionType.LOGIN_NATIVE
          ip = "10.10.0.1"
          countryCode = "CZ"
          country = "Czechia"
          city = "Prague"
          userAgent = EDGE_WINDOWS_UA
          lastUsedAt = Date(1661342685000)
          expiresAt = activeExpiry
          edgeSession = this
          createdAtByDeviceId[deviceId] = Date(1661242685000)
        }

        addSession {
          type = UserSessionType.LOGIN_GITHUB
          ip = "10.10.0.2"
          countryCode = "CZ"
          country = "Czechia"
          city = "Brno"
          userAgent = OPERA_LINUX_UA
          lastUsedAt = Date(1661342785000)
          expiresAt = activeExpiry
          operaSession = this
          createdAtByDeviceId[deviceId] = Date(1661242785000)
        }

        addSession {
          type = UserSessionType.LOGIN_GOOGLE
          ip = "10.10.0.3"
          countryCode = "GB"
          country = "United Kingdom"
          city = "London"
          userAgent = CHROME_WINDOWS_UA
          lastUsedAt = Date(1661342885000)
          expiresAt = activeExpiry
          chromeWindowsSession = this
          createdAtByDeviceId[deviceId] = Date(1661242885000)
        }

        addSession {
          type = UserSessionType.LOGIN_OAUTH2
          ip = "10.10.0.4"
          countryCode = "US"
          country = "United States"
          city = "New York"
          userAgent = FIREFOX_WINDOWS_UA
          lastUsedAt = Date(1661342985000)
          expiresAt = activeExpiry
          firefoxSession = this
          createdAtByDeviceId[deviceId] = Date(1661242985000)
        }

        addSession {
          type = UserSessionType.LOGIN_SSO
          ip = "10.10.0.5"
          countryCode = "US"
          country = "United States"
          city = "San Francisco"
          userAgent = SAFARI_IPHONE_UA
          lastUsedAt = Date(1661343085000)
          expiresAt = activeExpiry
          iphoneSession = this
          createdAtByDeviceId[deviceId] = Date(1661243085000)
        }

        addSession {
          type = UserSessionType.SIGN_UP
          ip = "10.10.0.6"
          countryCode = "JP"
          country = "Japan"
          city = "Tokyo"
          userAgent = CHROME_ANDROID_UA
          lastUsedAt = Date(1661343185000)
          expiresAt = activeExpiry
          androidSession = this
          createdAtByDeviceId[deviceId] = Date(1661243185000)
        }

        addSession {
          type = UserSessionType.EMAIL_VERIFICATION
          ip = "10.10.0.7"
          countryCode = "AU"
          country = "Australia"
          city = "Sydney"
          userAgent = CHROME_MAC_UA
          lastUsedAt = Date(1661343285000)
          expiresAt = activeExpiry
          chromeMacSession = this
          createdAtByDeviceId[deviceId] = Date(1661243285000)
        }

        addSession {
          type = UserSessionType.IMPERSONATION
          ip = "10.10.0.8"
          countryCode = "FR"
          country = "France"
          city = "Paris"
          userAgent = SAFARI_MAC_UA
          lastUsedAt = Date(1661343385000)
          expiresAt = activeExpiry
          impersonationSession = this
          createdAtByDeviceId[deviceId] = Date(1661243385000)
        }

        addSession {
          type = UserSessionType.TEST
          ip = "10.10.0.9"
          userAgent = GARBAGE_UA
          lastUsedAt = Date(1661343485000)
          expiresAt = activeExpiry
          garbageAgentSession = this
          createdAtByDeviceId[deviceId] = Date(1661243485000)
        }

        addSession {
          type = UserSessionType.UNKNOWN
          ip = "10.10.0.10"
          countryCode = "ES"
          country = "Spain"
          city = "Madrid"
          userAgent = FIREFOX_LINUX_UA
          lastUsedAt = Date(1661343585000)
          expiresAt = activeExpiry
          firefoxLinuxSession = this
          createdAtByDeviceId[deviceId] = Date(1661243585000)
        }

        addSession {
          type = UserSessionType.UNKNOWN
          ip = "10.10.0.11"
          countryCode = "NL"
          country = "Netherlands"
          userAgent = null
          lastUsedAt = null
          expiresAt = activeExpiry
          nullAgentSession = this
          createdAtByDeviceId[deviceId] = Date(1661243685000)
        }

        addSession {
          type = UserSessionType.LOGIN_NATIVE
          ip = "10.10.0.12"
          countryCode = "CA"
          country = "Canada"
          city = "Toronto"
          userAgent = CHROME_WINDOWS_UA
          lastUsedAt = Date(1661343785000)
          expiresAt = activeExpiry
          revokedAt = currentDateProvider.date.addDays(-1)
          revokedSession = this
          createdAtByDeviceId[deviceId] = Date(1661243785000)
        }

        addSession {
          type = UserSessionType.LOGIN_NATIVE
          ip = "10.10.0.13"
          countryCode = "SE"
          country = "Sweden"
          city = "Stockholm"
          userAgent = CHROME_WINDOWS_UA
          lastUsedAt = Date(1661343885000)
          expiresAt = currentDateProvider.date.addDays(-1)
          expiredSession = this
          createdAtByDeviceId[deviceId] = Date(1661243885000)
        }
      }
    }

  companion object {
    const val USERNAME = "user@user.com"
    const val OTHER_USERNAME = "peter@peter.com"
    const val UNVERIFIED_USERNAME = "ursula@ursula.com"

    const val EDGE_WINDOWS_UA =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36 Edg/124.0.2478.67"

    const val OPERA_LINUX_UA =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/123.0.0.0 Safari/537.36 OPR/109.0.5097.45"

    const val CHROME_WINDOWS_UA =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

    const val FIREFOX_WINDOWS_UA =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"

    const val FIREFOX_LINUX_UA =
      "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0"

    const val SAFARI_IPHONE_UA =
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
        "Version/17.4 Mobile/15E148 Safari/604.1"

    const val CHROME_ANDROID_UA =
      "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.6367.82 Mobile Safari/537.36"

    const val CHROME_MAC_UA =
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

    const val SAFARI_MAC_UA =
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
        "Version/17.4.1 Safari/605.1.15"

    const val GARBAGE_UA = "!!! not-a-user-agent 42 !!!"
  }
}
