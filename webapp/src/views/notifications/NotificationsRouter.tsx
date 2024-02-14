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

import React from 'react';
import { Switch } from 'react-router-dom';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import { NotificationsView } from 'tg.views/notifications/NotificationsView';
import { useTranslate } from '@tolgee/react';

export const NotificationsRouter: React.FC = () => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <Switch>
        <PrivateRoute exact path={LINKS.NOTIFICATIONS_INBOX.template}>
          <NotificationsView
            unread
            read
            navigation={[
              t('notifications-inbox'),
              LINKS.NOTIFICATIONS_INBOX.build(),
            ]}
          />
        </PrivateRoute>

        <PrivateRoute exact path={LINKS.NOTIFICATIONS_UNREAD.template}>
          <NotificationsView
            unread
            navigation={[
              t('notifications-unread'),
              LINKS.NOTIFICATIONS_UNREAD.build(),
            ]}
          />
        </PrivateRoute>

        <PrivateRoute exact path={LINKS.NOTIFICATIONS_DONE.template}>
          <NotificationsView
            done
            navigation={[
              t('notifications-done'),
              LINKS.NOTIFICATIONS_DONE.build(),
            ]}
          />
        </PrivateRoute>
      </Switch>
    </DashboardPage>
  );
};
