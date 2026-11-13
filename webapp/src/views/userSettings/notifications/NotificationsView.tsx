import React from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { useTranslate } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';
import { SettingsRow } from 'tg.views/userSettings/notifications/SettingsRow';
import { components } from 'tg.service/apiSchema.generated';
import { LocalizationSettingsRow } from 'tg.views/userSettings/notifications/LocalizationSettingsRow';
import { WorkEmailFrequencyRow } from 'tg.views/userSettings/notifications/WorkEmailFrequencyRow';
import { NotificationLanguagesSection } from 'tg.views/userSettings/notifications/NotificationLanguagesSection';
import { NotificationMentionProjectsSection } from 'tg.views/userSettings/notifications/NotificationMentionProjectsSection';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

const StyledRoot = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 120px 120px;
  grid-auto-rows: auto;
  grid-row-gap: 10px;
`;

const StyledGroupTitle = styled(Typography)`
  grid-column: 1 / -1;
  margin-top: ${({ theme }) => theme.spacing(2)};
  font-weight: 500;
`;

const StyledDivider = styled(Box)`
  grid-column: 1 / -1;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  margin-top: ${({ theme }) => theme.spacing(2)};
`;

type LocalizationType =
  components['schemas']['NotificationTypeSettingModel']['type'];

const WORK_TYPES: LocalizationType[] = [
  'KEYS_ADDED',
  'AUTOMATICALLY_TRANSLATED',
  'SOURCE_CHANGED',
];

const OTHER_CHANGE_TYPES: LocalizationType[] = [
  'STRINGS_TRANSLATED',
  'STRINGS_REVIEWED',
];

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
  const afterChange = () => settingsLoadable.refetch();

  const localizationCopy: Partial<
    Record<LocalizationType, { label: string; description: string }>
  > = {
    KEYS_ADDED: {
      label: t('settings_notifications_type_keys_added', 'New keys'),
      description: t(
        'settings_notifications_type_keys_added_description',
        'New strings to translate in your languages'
      ),
    },
    AUTOMATICALLY_TRANSLATED: {
      label: t(
        'settings_notifications_type_auto_translated',
        'Auto-translated'
      ),
      description: t(
        'settings_notifications_type_auto_translated_description',
        'Auto-translated strings to review'
      ),
    },
    SOURCE_CHANGED: {
      label: t(
        'settings_notifications_type_source_changed',
        'Source text changed'
      ),
      description: t(
        'settings_notifications_type_source_changed_description',
        'Existing strings need an update'
      ),
    },
    STRINGS_TRANSLATED: {
      label: t(
        'settings_notifications_type_strings_translated',
        'Translated by others'
      ),
      description: t(
        'settings_notifications_type_strings_translated_description',
        'Someone translated strings in your languages'
      ),
    },
    STRINGS_REVIEWED: {
      label: t(
        'settings_notifications_type_strings_reviewed',
        'Reviewed by others'
      ),
      description: t(
        'settings_notifications_type_strings_reviewed_description',
        'Someone reviewed strings in your languages'
      ),
    },
  };

  const renderLocalizationRows = (types: LocalizationType[]) =>
    types.map((type) => {
      const setting = settings.localization.find((s) => s.type === type);
      if (!setting) {
        return null;
      }
      return (
        <LocalizationSettingsRow
          key={type}
          setting={setting}
          label={localizationCopy[type]?.label ?? type}
          description={localizationCopy[type]?.description}
          afterChange={afterChange}
        />
      );
    });

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
        <StyledGroupTitle variant="subtitle1">
          {t('settings_notifications_group_comments', 'Comments')}
        </StyledGroupTitle>
        <SettingsRow
          description={t(
            'settings_notifications_mentions_description',
            'Mentions'
          )}
          subdescription={t(
            'settings_notifications_mentions_subdescription',
            'Someone mentions you in a comment'
          )}
          group="COMMENTS"
          channels={settings.comments}
          afterChange={afterChange}
        />
        <NotificationMentionProjectsSection />
        <StyledGroupTitle variant="subtitle1">
          {t('settings_notifications_group_work', 'Work for you')}
        </StyledGroupTitle>
        {renderLocalizationRows(WORK_TYPES)}
        <NotificationLanguagesSection />
        <StyledGroupTitle variant="subtitle1">
          {t('settings_notifications_group_other_changes', 'Other changes')}
        </StyledGroupTitle>
        {renderLocalizationRows(OTHER_CHANGE_TYPES)}
        <StyledGroupTitle variant="subtitle1">
          {t('settings_notifications_group_other', 'Other')}
        </StyledGroupTitle>
        {tasksEnabled && (
          <SettingsRow
            description={t('settings_notifications_tasks_description')}
            subdescription={t('settings_notifications_tasks_subdescription')}
            group="TASKS"
            channels={settings.tasks}
            afterChange={afterChange}
          />
        )}
        <SettingsRow
          description={t('settings_notifications_account_security_description')}
          subdescription={t(
            'settings_notifications_account_security_subdescription'
          )}
          group="ACCOUNT_SECURITY"
          channels={settings.accountSecurity}
          disabledInApp={true}
          disabledEmail={true}
          afterChange={afterChange}
        />
        <StyledDivider />
        <WorkEmailFrequencyRow
          value={settings.digestFrequency}
          afterChange={afterChange}
        />
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
