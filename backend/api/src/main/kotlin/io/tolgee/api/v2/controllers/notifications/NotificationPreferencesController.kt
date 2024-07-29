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
import io.tolgee.notifications.NotificationPreferencesService
import io.tolgee.notifications.dto.NotificationPreferencesDto
import io.tolgee.security.authentication.AuthenticationFacade
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v2/notifications/preferences"])
@Tag(name = "Notification preferences")
class NotificationPreferencesController(
  private val authenticationFacade: AuthenticationFacade,
  private val notificationPreferencesService: NotificationPreferencesService,
) {
  @GetMapping("")
  @Operation(summary = "Fetch the global preferences and all overrides of the current user")
  fun getAllPreferences(): Map<String, NotificationPreferencesDto> {
    return notificationPreferencesService.getAllPreferences(authenticationFacade.authenticatedUser.id)
  }

  @GetMapping("/global")
  @Operation(summary = "Fetch the global preferences for the current user")
  fun getGlobalPreferences(): NotificationPreferencesDto {
    return notificationPreferencesService.getGlobalPreferences(authenticationFacade.authenticatedUser.id)
  }

  @PutMapping("/global")
  @Operation(summary = "Update the global notification preferences of the current user")
  fun updateGlobalPreferences(
    @RequestBody @Validated preferencesDto: NotificationPreferencesDto,
  ): NotificationPreferencesDto {
    val updated =
      notificationPreferencesService.setPreferencesOfUser(
        authenticationFacade.authenticatedUser.id,
        preferencesDto,
      )

    return NotificationPreferencesDto.fromEntity(updated)
  }

  @GetMapping("/project/{id}")
  @Operation(summary = "Fetch the notification preferences of the current user for a specific project")
  fun getPerProjectPreferences(
    @PathVariable("id") id: Long,
  ): NotificationPreferencesDto {
    return notificationPreferencesService.getProjectPreferences(
      authenticationFacade.authenticatedUser.id,
      id,
    )
  }

  @PutMapping("/project/{id}")
  @Operation(summary = "Update the notification preferences of the current user for a specific project")
  fun updatePerProjectPreferences(
    @PathVariable("id") id: Long,
    @RequestBody @Validated preferencesDto: NotificationPreferencesDto,
  ): NotificationPreferencesDto {
    val updated =
      notificationPreferencesService.setProjectPreferencesOfUser(
        authenticationFacade.authenticatedUser.id,
        id,
        preferencesDto,
      )

    return NotificationPreferencesDto.fromEntity(updated)
  }

  @DeleteMapping("/project/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete the notification preferences of the current user for a specific project")
  fun deletePerProjectPreferences(
    @PathVariable("id") id: Long,
  ) {
    notificationPreferencesService.deleteProjectPreferencesOfUser(
      authenticationFacade.authenticatedUser.id,
      id,
    )
  }

  @PostMapping("/project/{id}/subscribe")
  @Operation(summary = "Subscribe to notifications for a given project")
  fun subscribeToProject(
    @PathVariable("id") id: Long,
  ): ResponseEntity<String> {
    return ResponseEntity(
      "Coming soon! Please see https://github.com/tolgee/tolgee-platform/issues/1360 for progress on this. :D",
      HttpHeaders().also {
        @Suppress("UastIncorrectHttpHeaderInspection")
        it.add(
          "x-hey-curious-reader",
          "oh hey there, didn't expect you here... " +
            "if you're here, might as well join us! https://tolgee.io/career",
        )
      },
      HttpStatus.NOT_IMPLEMENTED,
    )
  }
}
