import React from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { useTranslate } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';
import { SettingsRow } from 'tg.views/userSettings/notifications/SettingsRow';
import { components } from 'tg.service/apiSchema.generated';
import { LocalizationSettingsRow } from 'tg.views/userSettings/notifications/LocalizationSettingsRow';
import { DigestFrequencySelect } from 'tg.views/userSettings/notifications/DigestFrequencySelect';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

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

export const NotificationsView: React.FC<
  React.PropsWithChildren<unknown>
> = () => {
  const { t } = useTranslate();
  const { isEnabled } = useEnabledFeatures();

  const settingsLoadable = useApiQuery({
    url: '/v2/notification-settings',
    method: 'get',
    options: { keepPreviousData: true },
  });

  const settings = settingsLoadable.data;

  if (!settings) {
    return null;
  }

  const tasksEnabled = isEnabled('TASKS');

  const localizationLabels: Partial<
    Record<
      components['schemas']['NotificationTypeSettingModel']['type'],
      string
    >
  > = {
    KEYS_ADDED: t('notifications-keys-added', 'New keys added'),
    SOURCE_CHANGED: t('notifications-source-changed', 'Base text changed'),
    STRINGS_TRANSLATED: t(
      'notifications-strings-translated',
      'Strings translated'
    ),
    STRINGS_REVIEWED: t('notifications-strings-reviewed', 'Strings reviewed'),
    AUTOMATICALLY_TRANSLATED: t(
      'notifications-automatically-translated',
      'Automatically translated'
    ),
    BULK_CHANGED: t('notifications-bulk-changed', 'Changed in bulk'),
  };

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
      hideChildrenOnLoading={false}
    >
      <StyledRoot>
        <Box></Box>
        <StyledTableHeader>
          {t('settings_notifications_channel_in_app')}
        </StyledTableHeader>
        <StyledTableHeader>
          {t('settings_notifications_channel_email')}
        </StyledTableHeader>
        <SettingsRow
          description={t('settings_notifications_account_security_description')}
          subdescription={t(
            'settings_notifications_account_security_subdescription'
          )}
          group="ACCOUNT_SECURITY"
          channels={settings.accountSecurity}
          disabledInApp={true}
          disabledEmail={true}
          afterChange={() => settingsLoadable.refetch()}
        />
        {tasksEnabled && (
          <SettingsRow
            description={t('settings_notifications_tasks_description')}
            subdescription={t('settings_notifications_tasks_subdescription')}
            group="TASKS"
            channels={settings.tasks}
            afterChange={() => settingsLoadable.refetch()}
          />
        )}
        <Box sx={{ gridColumn: '1 / -1', mt: 2 }}>
          <Typography variant="h6">
            {t(
              'settings_notifications_localization_title',
              'Localization changes'
            )}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t(
              'settings_notifications_localization_subtitle',
              'Email notifications are sent as one summary, at most once a day.'
            )}
          </Typography>
        </Box>
        <Box sx={{ gridColumn: '1 / -1' }}>
          <DigestFrequencySelect
            value={settings.digestFrequency}
            afterChange={() => settingsLoadable.refetch()}
          />
        </Box>
        {settings.localization.map((setting) => (
          <LocalizationSettingsRow
            key={setting.type}
            setting={setting}
            label={localizationLabels[setting.type] ?? setting.type}
            afterChange={() => settingsLoadable.refetch()}
          />
        ))}
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
