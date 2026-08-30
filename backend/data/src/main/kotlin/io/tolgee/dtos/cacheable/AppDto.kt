package io.tolgee.dtos.cacheable

import io.tolgee.model.apps.App
import java.io.Serializable

/**
 * The app-auth view of an [App]: only the force-revoke cutoff the token check needs. Kept minimal so
 * a manifest refresh, which never touches [App.tokensInvalidBefore], does not have to evict it.
 */
data class AppDto(
  val id: Long,
  val tokensInvalidBefore: Long?,
) : Serializable {
  companion object {
    fun fromEntity(app: App): AppDto =
      AppDto(
        id = app.id,
        tokensInvalidBefore = app.tokensInvalidBefore?.time,
      )
  }
}
