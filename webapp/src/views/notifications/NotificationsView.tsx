/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
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

import { LINKS } from 'tg.constants/links';

import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { NavigationItem } from 'tg.component/navigation/Navigation';

type Props = {
  unread?: boolean;
  read?: boolean;
  done?: boolean;
  navigation: NavigationItem;
};

export const NotificationsView: React.FC<Props> = ({
  navigation,
  unread,
  read,
  done,
}) => {
  const { t } = useTranslate();

  return (
    <BaseSettingsView
      windowTitle={'Notifications'}
      navigation={[
        [t('notifications'), LINKS.NOTIFICATIONS_INBOX.build()],
        navigation,
      ]}
      menuItems={[
        {
          link: LINKS.NOTIFICATIONS_INBOX.build(),
          label: t('notifications-inbox'),
        },
        {
          link: LINKS.NOTIFICATIONS_UNREAD.build(),
          label: t('notifications-unread'),
        },
        {
          link: LINKS.NOTIFICATIONS_DONE.build(),
          label: t('notifications-done'),
        },
      ]}
    >
      <p>test</p>
    </BaseSettingsView>
  );
};
