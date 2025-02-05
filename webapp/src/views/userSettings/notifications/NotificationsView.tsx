import React from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';
import { SettingsRow } from 'tg.views/userSettings/notifications/SettingsRow';

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
  return (
    <BaseUserSettingsView
      windowTitle={t('settings_notifications_title')}
      title={t('settings_notifications_title')}
      loading={false} // TODO
      navigation={[
        [
          t('user_menu_notifications'),
          LINKS.USER_ACCOUNT_NOTIFICATIONS.build(),
        ],
      ]}
      hideChildrenOnLoading={false} // TODO
    >
      <StyledRoot>
        <Box></Box>
        <StyledTableHeader>In-App</StyledTableHeader>
        <StyledTableHeader>Email</StyledTableHeader>
        <SettingsRow
          description="Account security"
          subdescription="Password-changed, Two-Factor authentication on/off"
          disabledInApp={true}
          disabledEmail={true}
        />
        <SettingsRow
          description="Tasks"
          subdescription="Assigned, completed, closed"
        />
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
