/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.api.v2.controllers.notifications

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.notifications.UserNotificationModel
import io.tolgee.hateoas.notifications.UserNotificationModelAssembler
import io.tolgee.notifications.NotificationStatus
import io.tolgee.notifications.UserNotificationService
import io.tolgee.security.authentication.AuthenticationFacade
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v2/notifications"])
@Tag(name = "Notifications")
class NotificationsController(
  private val authenticationFacade: AuthenticationFacade,
  private val userNotificationService: UserNotificationService,
  private val userNotificationModelAssembler: UserNotificationModelAssembler,
) {
  @GetMapping("/")
  @Operation(summary = "Fetch the current user's notifications")
  fun getNotifications(
    @RequestParam("status", defaultValue = "UNREAD,READ") status: Set<NotificationStatus>,
    @ParameterObject pageable: Pageable,
  ): List<UserNotificationModel> {
    val notifications = userNotificationService.findNotificationsOfUserFilteredPaged(
      authenticationFacade.authenticatedUser.id,
      status,
      pageable,
    )

    return notifications.map { userNotificationModelAssembler.toModel(it) }
  }

  @PostMapping("/mark-as-read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Marks a given set of notifications as read.")
  fun markNotificationsAsRead(@RequestBody notifications: List<Long>) {
    userNotificationService.markAsRead(
      authenticationFacade.authenticatedUser.id,
      notifications,
    )
  }

  @PostMapping("/mark-as-read/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Marks all notifications as read.")
  fun markAllNotificationsAsRead() {
    userNotificationService.markAllAsRead(authenticationFacade.authenticatedUser.id)
  }

  @PostMapping("/mark-as-unread")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Marks a given set of notifications as unread.")
  fun markNotificationsAsUnread(@RequestBody notifications: List<Long>) {
    userNotificationService.markAsUnread(
      authenticationFacade.authenticatedUser.id,
      notifications,
    )
  }

  @PostMapping("/mark-as-done")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Marks a given set of notifications as done.")
  fun markNotificationsAsDone(@RequestBody notifications: List<Long>) {
    userNotificationService.markAsDone(
      authenticationFacade.authenticatedUser.id,
      notifications,
    )
  }

  @PostMapping("/mark-as-done/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Marks all notifications as done.")
  fun markAllNotificationsAsDone() {
    userNotificationService.markAllAsDone(authenticationFacade.authenticatedUser.id)
  }

  @PostMapping("/unmark-as-done")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Un-marks a given set of notifications as done.")
  fun unmarkNotificationsAsDone(@RequestBody notifications: Collection<Long>) {
    userNotificationService.unmarkAsDone(
      authenticationFacade.authenticatedUser.id,
      notifications,
    )
  }
}
