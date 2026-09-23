package io.tolgee.repository.oauth2

import io.tolgee.model.oauth2.OAuth2ClientDocumentCheck
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface OAuth2ClientDocumentCheckRepository : JpaRepository<OAuth2ClientDocumentCheck, Long> {
  fun findByClientId(clientId: String): OAuth2ClientDocumentCheck?

  /** A client nobody holds a grant for any more is never checked again, so its attempt row is dead weight. */
  @Modifying
  @Query(
    """
    DELETE FROM OAuth2ClientDocumentCheck c
    WHERE NOT EXISTS (SELECT 1 FROM OAuth2Grant g WHERE g.clientId = c.clientId)
    """,
  )
  fun deleteWithoutGrants(): Int
}
