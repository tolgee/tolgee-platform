package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.hateoas.notification.NotificationEnhancer
import io.tolgee.hateoas.notification.NotificationModel
import io.tolgee.hateoas.notification.NotificationModelAssembler
import io.tolgee.hateoas.notification.NotificationWebsocketModel
import io.tolgee.model.Notification
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
import org.springframework.hateoas.PagedModel
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
  @GetMapping
  @Operation(summary = "Gets notifications of the currently logged in user, newest is first.")
  @AllowApiAccess
  fun getNotifications(
    @ParameterObject
    @SortDefault(sort = ["id"], direction = Sort.Direction.DESC)
    pageable: Pageable,
    @ParameterObject
    filters: NotificationFilters = NotificationFilters(),
  ): PagedModel<NotificationModel> {
    val notifications =
      notificationService.getNotifications(
        authenticationFacade.authenticatedUser.id,
        pageable,
        filters,
      )
    return pagedResourcesAssembler.toModel(
      notifications,
      NotificationModelAssembler(enhancers, notifications),
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
