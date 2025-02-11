import React from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';
import { SettingsRow } from 'tg.views/userSettings/notifications/SettingsRow';
import { useApiQuery } from 'tg.service/http/useQueryApi';

const StyledRoot = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 120px 120px;
  grid-auto-rows: auto;
  grid-row-gap: 10px;
`;

const StyledTableHeader = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
  text-align: center;
`;

export const NotificationsView: React.FC = () => {
  const { t } = useTranslate();
  const settingsLoadable = useApiQuery({
    url: '/v2/notifications-settings',
    method: 'get',
  });

  const settings = settingsLoadable.data;

  if (!settings) {
    return null;
  }

  return (
    <BaseUserSettingsView
      windowTitle={t('settings_notifications_title')}
      title={t('settings_notifications_title')}
      loading={settingsLoadable.isFetching}
      navigation={[
        [
          t('user_menu_notifications'),
          LINKS.USER_ACCOUNT_NOTIFICATIONS.build(),
        ],
      ]}
    >
      <StyledRoot>
        <Box></Box>
        <StyledTableHeader>In-App</StyledTableHeader>
        <StyledTableHeader>Email</StyledTableHeader>
        <SettingsRow
          description="Account security"
          subdescription="Password-changed, Two-Factor authentication on/off"
          group="ACCOUNT_SECURITY"
          channels={settings.items['ACCOUNT_SECURITY']}
          disabledInApp={true}
          disabledEmail={true}
          afterChange={() => settingsLoadable.refetch()}
        />
        <SettingsRow
          description="Tasks"
          subdescription="Assigned, completed, closed"
          group="TASKS"
          channels={settings.items['TASKS']}
          afterChange={() => settingsLoadable.refetch()}
        />
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
