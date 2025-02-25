package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.PagedModelWithNextCursor
import io.tolgee.hateoas.notification.NotificationEnhancer
import io.tolgee.hateoas.notification.NotificationModel
import io.tolgee.hateoas.notification.NotificationModelAssembler
import io.tolgee.hateoas.notification.NotificationWebsocketModel
import io.tolgee.model.notifications.Notification
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationService
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import io.tolgee.websocket.WebsocketEventType
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.data.web.SortDefault.SortDefaults
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/notifications",
  ],
)
@Tag(name = "Notifications", description = "Manipulates notifications")
class NotificationController(
  private val notificationService: NotificationService,
  private val authenticationFacade: AuthenticationFacade,
  private val enhancers: List<NotificationEnhancer>,
  private val pagedResourcesAssembler: PagedResourcesAssembler<Notification>,
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val currentDateProvider: CurrentDateProvider,
) {
  private val defaultSort: Sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))

  @GetMapping
  @Operation(summary = "Gets notifications of the currently logged in user, newest is first.")
  @AllowApiAccess
  fun getNotifications(
    @ParameterObject
    @SortDefaults(
      SortDefault(sort = ["createdAt"], direction = Sort.Direction.DESC),
      SortDefault(sort = ["id"], direction = Sort.Direction.DESC),
    )
    pageable: Pageable,
    @ParameterObject
    filters: NotificationFilters = NotificationFilters(),
    cursor: String? = null,
  ): PagedModelWithNextCursor<NotificationModel> {
    if (cursor != null &&
      pageable.pageNumber > 0 &&
      !pageable.sort.equals(defaultSort)
    ) {
      throw BadRequestException(Message.SORTING_AND_PAGING_IS_NOT_SUPPORTED_WHEN_USING_CURSOR)
    }

    val notifications =
      notificationService.getNotifications(
        authenticationFacade.authenticatedUser.id,
        pageable,
        filters,
        cursor?.let { String(Base64.getDecoder().decode(it)).toLong() },
      )
    val model =
      pagedResourcesAssembler.toModel(
        notifications,
        NotificationModelAssembler(enhancers, notifications),
      )
    return PagedModelWithNextCursor(
      model,
      model.content
        .let { if (it.isEmpty()) null else it.last() }
        ?.id
        ?.toString()?.let { Base64.getEncoder().encodeToString(it.toByteArray()) },
    )
  }

  @TransactionalEventListener
  fun onNotificationsChanged(event: OnNotificationsChangedForUser) {
    val websocketModel =
      NotificationWebsocketModel(
        notificationService.getCountOfUnseenNotifications(event.userId),
        event.newNotification?.let {
          NotificationModelAssembler(enhancers, PageImpl(listOf(it))).toModel(it)
        },
      )

    val websocketEvent =
      WebsocketEvent(
        data = websocketModel,
        timestamp = currentDateProvider.date.time,
      )

    websocketEventPublisher(
      "/users/${event.userId}/${WebsocketEventType.NOTIFICATIONS_CHANGED.typeName}",
      websocketEvent,
    )
  }
}
