package io.tolgee.events

/**
 * A project's content was replaced wholesale by a path that bypasses the activity interceptor —
 * bulk JPQL deletes and entities written with activity logging disabled produce no
 * [OnProjectActivityEvent], so usage that is derived from content has to be told directly.
 */
class OnProjectContentReplaced(
  val projectId: Long,
)
