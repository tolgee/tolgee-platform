import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';
import { LINKS } from 'tg.constants/links';

import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { ConnectedAppListItem } from './ConnectedAppListItem';

export const ConnectedAppsView: FunctionComponent<
  React.PropsWithChildren<unknown>
> = () => {
  const { t } = useTranslate();

  const list = useApiQuery({
    url: '/v2/user/connected-apps',
    method: 'get',
  });

  const apps = list.data ?? [];

  return (
    <BaseUserSettingsView
      windowTitle={t('connected_apps_title', 'Connected apps')}
      title={t('connected_apps_title', 'Connected apps')}
      loading={list.isFetching}
      navigation={[
        [
          t('user_menu_connected_apps', 'Connected apps'),
          LINKS.USER_CONNECTED_APPS.build(),
        ],
      ]}
      hideChildrenOnLoading={false}
    >
      <Box sx={{ my: 2 }}>
        <T
          keyName="connected_apps_description"
          defaultValue="Apps you have authorized to access your Tolgee account through OAuth. Disconnecting an app revokes all of its access; the next time you connect it, you can review and change what it may access."
        />
      </Box>
      <Box className="listWrapper" data-cy="connected-apps-list">
        {apps.length ? (
          apps.map((app) => (
            <ConnectedAppListItem key={app.clientId} app={app} />
          ))
        ) : (
          <EmptyListMessage loading={list.isFetching}>
            <T
              keyName="connected_apps_empty_message"
              defaultValue="You haven't connected any apps yet."
            />
          </EmptyListMessage>
        )}
      </Box>
    </BaseUserSettingsView>
  );
};
