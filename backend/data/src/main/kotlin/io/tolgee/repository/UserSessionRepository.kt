package io.tolgee.repository

import io.tolgee.model.UserSession
import io.tolgee.model.enums.UserSessionType
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface UserSessionRepository : JpaRepository<UserSession, Long> {
  fun findByDeviceId(deviceId: String): UserSession?

  /**
   * Sessions the user may see: not revoked, not expired, not invalidated by a `tokensValidNotBefore`
   * bump, and never an impersonation session (support access is not disclosed to the account owner).
   */
  @Query(
    """
    select us from UserSession us
    where us.userAccountId = :userAccountId
      and us.revokedAt is null
      and us.expiresAt > :now
      and us.type <> :impersonationType
      and coalesce(us.lastRefreshedAt, us.createdAt) >= :tokensValidNotBefore
    order by coalesce(us.lastUsedAt, us.createdAt) desc
    """,
  )
  fun findActive(
    @Param("userAccountId") userAccountId: Long,
    @Param("now") now: Date,
    @Param("tokensValidNotBefore") tokensValidNotBefore: Date,
    @Param("impersonationType") impersonationType: UserSessionType,
    pageable: Pageable,
  ): Page<UserSession>

  /**
   * Everything `DELETE /other` sweeps. Unlike the listing this ignores expiry and
   * `tokensValidNotBefore`, so dead rows cannot linger or be resurrected by a backfill.
   */
  @Query(
    """
    select us from UserSession us
    where us.userAccountId = :userAccountId
      and us.revokedAt is null
      and us.type <> :impersonationType
      and us.deviceId <> :exceptDeviceId
    """,
  )
  fun findRevocable(
    @Param("userAccountId") userAccountId: Long,
    @Param("exceptDeviceId") exceptDeviceId: String,
    @Param("impersonationType") impersonationType: UserSessionType,
  ): List<UserSession>

  @Modifying
  @Query(
    """
    update UserSession us
    set us.revokedAt = :now, us.revokedById = :revokedById, us.updatedAt = :now
    where us.id in :ids and us.revokedAt is null
    """,
  )
  fun revokeAllByIdIn(
    @Param("ids") ids: Collection<Long>,
    @Param("now") now: Date,
    @Param("revokedById") revokedById: Long,
  ): Int

  @Modifying
  @Query(
    """
    update UserSession us set us.lastUsedAt = :date
    where us.deviceId = :deviceId and us.revokedAt is null
    """,
  )
  fun updateLastUsed(
    @Param("deviceId") deviceId: String,
    @Param("date") date: Date,
  ): Int

  /**
   * Atomic insert-or-refresh keyed on the device id. `type`, `revoked_at` and `revoked_by_id` are
   * never overwritten: a refresh must not relabel the session's origin nor undo a revocation, and
   * `last_refreshed_at` only moves when the caller is actually refreshing.
   */
  @Modifying(flushAutomatically = true)
  @Query(
    value = """
    insert into user_session
      (id, created_at, updated_at, device_id, user_account_id, type, ip, user_agent, expires_at,
       last_refreshed_at, acting_user_account_id, country_code, country, city)
    values
      (:id, :now, :now, :deviceId, :userAccountId, :type, :ip, :userAgent, :expiresAt,
       :lastRefreshedAt, :actingUserAccountId, :countryCode, :country, :city)
    on conflict (device_id) do update set
      expires_at = excluded.expires_at,
      ip = excluded.ip,
      user_agent = excluded.user_agent,
      updated_at = excluded.updated_at,
      country_code = excluded.country_code,
      country = excluded.country,
      city = excluded.city,
      last_refreshed_at = coalesce(excluded.last_refreshed_at, user_session.last_refreshed_at)
    """,
    nativeQuery = true,
  )
  fun upsert(
    @Param("id") id: Long,
    @Param("now") now: Date,
    @Param("deviceId") deviceId: String,
    @Param("userAccountId") userAccountId: Long,
    @Param("type") type: String,
    @Param("ip") ip: String?,
    @Param("userAgent") userAgent: String?,
    @Param("expiresAt") expiresAt: Date,
    @Param("lastRefreshedAt") lastRefreshedAt: Date?,
    @Param("actingUserAccountId") actingUserAccountId: Long?,
    @Param("countryCode") countryCode: String?,
    @Param("country") country: String?,
    @Param("city") city: String?,
  )

  @Modifying(flushAutomatically = true)
  @Query(
    value = """
    insert into user_session
      (id, created_at, updated_at, device_id, user_account_id, type, ip, user_agent, expires_at,
       acting_user_account_id, country_code, country, city)
    values
      (:id, :now, :now, :deviceId, :userAccountId, :type, :ip, :userAgent, :expiresAt,
       :actingUserAccountId, :countryCode, :country, :city)
    on conflict (device_id) do nothing
    """,
    nativeQuery = true,
  )
  fun insertIfAbsent(
    @Param("id") id: Long,
    @Param("now") now: Date,
    @Param("deviceId") deviceId: String,
    @Param("userAccountId") userAccountId: Long,
    @Param("type") type: String,
    @Param("ip") ip: String?,
    @Param("userAgent") userAgent: String?,
    @Param("expiresAt") expiresAt: Date,
    @Param("actingUserAccountId") actingUserAccountId: Long?,
    @Param("countryCode") countryCode: String?,
    @Param("country") country: String?,
    @Param("city") city: String?,
  )

  @Query("select us.id from UserSession us where us.expiresAt < :cutoff and us.id > :afterId order by us.id")
  fun findIdsToPurge(
    @Param("cutoff") cutoff: Date,
    @Param("afterId") afterId: Long,
    pageable: Pageable,
  ): List<Long>

  fun deleteAllByIdIn(ids: Collection<Long>)

  fun deleteAllByUserAccountId(userAccountId: Long)
}
