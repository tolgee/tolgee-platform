package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.notification.NotificationEnhancer
import io.tolgee.hateoas.notification.NotificationModelAssembler
import io.tolgee.hateoas.notification.NotificationPagedModel
import io.tolgee.model.Notification
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.web.bind.annotation.*

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
) {
  @GetMapping
  @Operation(summary = "Gets notifications of the currently logged in user, newest is first.")
  @AllowApiAccess
  fun getNotifications(
    @ParameterObject pageable: Pageable,
  ): NotificationPagedModel {
    val notifications = notificationService.getNotifications(authenticationFacade.authenticatedUser.id, pageable)
    val unseenCount =
      notificationService.getCountOfUnseenNotifications(authenticationFacade.authenticatedUser.id)
    val pagedNotifications = pagedResourcesAssembler.toModel(notifications, NotificationModelAssembler(enhancers, notifications))
    return NotificationPagedModel.of(pagedNotifications, unseenCount)
  }

  @PutMapping("/mark-seen")
  @Operation(summary = "Marks notifications of the currently logged in user with given IDs as seen.")
  @AllowApiAccess
  fun markNotificationsAsSeen(
    @RequestBody notificationIds: List<Long>,
  ) {
    notificationService.markNotificationsAsSeen(notificationIds, authenticationFacade.authenticatedUser.id)
  }
}
