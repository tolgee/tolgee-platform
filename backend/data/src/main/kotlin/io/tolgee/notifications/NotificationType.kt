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

package io.tolgee.notifications

import java.util.*

enum class NotificationType {
  ACTIVITY_LANGUAGES_CREATED,
  ACTIVITY_KEYS_CREATED,
  ACTIVITY_KEYS_UPDATED,
  ACTIVITY_KEYS_SCREENSHOTS_UPLOADED,
  ACTIVITY_SOURCE_STRINGS_UPDATED,
  ACTIVITY_TRANSLATIONS_UPDATED,
  ACTIVITY_TRANSLATION_OUTDATED,
  ACTIVITY_TRANSLATION_REVIEWED,
  ACTIVITY_TRANSLATION_UNREVIEWED,
  ACTIVITY_NEW_COMMENTS,
  ACTIVITY_COMMENTS_MENTION,

  BATCH_JOB_ERRORED,
  ;

  companion object {
    val ACTIVITY_NOTIFICATIONS: EnumSet<NotificationType> = EnumSet.of(
      ACTIVITY_LANGUAGES_CREATED,
      ACTIVITY_KEYS_CREATED,
      ACTIVITY_KEYS_UPDATED,
      ACTIVITY_KEYS_SCREENSHOTS_UPLOADED,
      ACTIVITY_SOURCE_STRINGS_UPDATED,
      ACTIVITY_TRANSLATIONS_UPDATED,
      ACTIVITY_TRANSLATION_OUTDATED,
      ACTIVITY_TRANSLATION_REVIEWED,
      ACTIVITY_TRANSLATION_UNREVIEWED,
      ACTIVITY_NEW_COMMENTS,
      ACTIVITY_COMMENTS_MENTION,
    )

    val BATCH_JOB_NOTIFICATIONS: EnumSet<NotificationType> = EnumSet.of(
      BATCH_JOB_ERRORED,
    )
  }
}
