package io.tolgee.util

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import io.tolgee.configuration.tolgee.TolgeeProperties
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Resolves an IP to a coarse location using a MaxMind-format database. No database file ships with
 * Tolgee: without a configured path this resolver stays disabled and every lookup returns null, so
 * the UI falls back to showing the raw IP.
 */
@Component
class GeoIpResolver(
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  private val readerLazy = lazy { openReader() }

  fun resolve(ip: String?): GeoIpLocation? {
    val address = parseLiteral(ip) ?: return null
    val reader = readerLazy.value ?: return null

    return try {
      val response = reader.tryCity(address).orElse(null) ?: return null
      GeoIpLocation(
        countryCode = response.country?.isoCode,
        country = response.country?.name,
        city = response.city?.name,
      )
    } catch (e: Exception) {
      // Private, loopback and unknown addresses are the common case here, not an error worth
      // logging on every authenticated request.
      logger.debug("GeoIP lookup failed for $ip", e)
      null
    }
  }

  private fun openReader(): DatabaseReader? {
    val path = tolgeeProperties.authentication.sessionAudit.geoIpDatabasePath
    if (path.isNullOrBlank()) return null

    val file = File(path)
    if (!file.isFile) {
      logger.warn("GeoIP database configured at $path but no such file exists; locations disabled")
      return null
    }

    return try {
      DatabaseReader.Builder(file).withCache(CHMCache()).build().also {
        logger.info("GeoIP database loaded from $path")
      }
    } catch (e: Exception) {
      logger.error("Failed to open GeoIP database at $path; locations disabled", e)
      null
    }
  }

  /**
   * `InetAddress.getByName` resolves anything that is not a literal, and the address comes from
   * attacker-controlled forwarding headers - so a hostname there would turn every authenticated
   * request into a DNS lookup. A colon cannot appear in a hostname, and the IPv4 branch is
   * digits-only, so neither form can reach a resolver.
   */
  private fun parseLiteral(ip: String?): InetAddress? {
    val value = ip?.trim()
    if (value.isNullOrEmpty()) return null
    if (!value.contains(':') && !IPV4.matches(value)) return null

    return try {
      InetAddress.getByName(value)
    } catch (e: UnknownHostException) {
      logger.debug("Not an IP literal: $value", e)
      null
    }
  }

  @PreDestroy
  fun close() {
    if (!readerLazy.isInitialized()) return
    runSentryCatching { readerLazy.value?.close() }
  }

  companion object {
    private val IPV4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")
  }
}
