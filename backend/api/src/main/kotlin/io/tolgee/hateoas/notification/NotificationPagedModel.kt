package io.tolgee.hateoas.notification

import org.springframework.hateoas.Link
import org.springframework.hateoas.Links
import org.springframework.hateoas.PagedModel

class NotificationPagedModel(
  content: Collection<NotificationModel> = emptyList(),
  metadata: PageMetadata? = null,
  links: Iterable<Link> = Links.NONE,
  val unseenCount: Int,
) : PagedModel<NotificationModel>(content, metadata, links) {
  companion object {
    fun of(original: PagedModel<NotificationModel>, unseenCount: Int): NotificationPagedModel {
      return NotificationPagedModel(original.content, original.metadata, original.links, unseenCount)
    }
  }
}
