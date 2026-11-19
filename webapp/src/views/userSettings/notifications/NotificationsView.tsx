import React, { useState } from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, styled, Tooltip, Typography } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';
import { components } from 'tg.service/apiSchema.generated';
import { WorkEmailFrequencyRow } from 'tg.views/userSettings/notifications/WorkEmailFrequencyRow';
import { NotificationProjectsSection } from 'tg.views/userSettings/notifications/NotificationProjectsSection';
import {
  Channel,
  ChannelRow,
} from 'tg.views/userSettings/notifications/ChannelRow';
import {
  clearPrototypeStorage,
  hasPrototypeOverrides,
  readJson,
  STORAGE_KEYS,
  writeJson,
} from 'tg.views/userSettings/notifications/prototypeStorage';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { confirmation } from 'tg.hooks/confirmation';

const StyledRoot = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 80px 110px;
  grid-auto-rows: auto;
  grid-row-gap: 12px;
  padding-bottom: ${({ theme }) => theme.spacing(8)};
`;

const StyledAlwaysOn = styled(Box)`
  align-self: center;
  justify-self: end;
  white-space: nowrap;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledSectionTitle = styled(Box)`
  font-size: 15px;
  font-weight: 500;
  text-transform: uppercase;
`;

const StyledSectionSpacer = styled(Box)`
  grid-column: 1 / -1;
  height: ${({ theme }) => theme.spacing(3)};
`;

const StyledTableHeader = styled(Box)`
  align-self: center;
  justify-self: end;
  width: 34px;
  display: flex;
  justify-content: center;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

type LocalizationType =
  components['schemas']['NotificationTypeSettingModel']['type'];
type Group = 'COMMENTS' | 'TASKS' | 'LOCALIZATION';
type Channels = { inApp: boolean; email: boolean };

type Item = {
  key: string;
  group: Group;
  type?: LocalizationType;
  channels: Channels;
  label: string;
  description?: string;
};

const MY_TRANSLATIONS = 'MY_TRANSLATIONS';
const MY_TRANSLATIONS_DEFAULT: Channels = { inApp: true, email: false };

const isDefaultChannels = (item: Item) =>
  item.channels.inApp &&
  item.channels.email === (item.group !== 'LOCALIZATION');

export const NotificationsView: React.FC<
  React.PropsWithChildren<unknown>
> = () => {
  const { t } = useTranslate();
  const [projectsKey, setProjectsKey] = useState(0);
  const [myTranslations, setMyTranslations] = useState<Channels>(() =>
    readJson<Channels>(STORAGE_KEYS.myTranslations, MY_TRANSLATIONS_DEFAULT)
  );

  const settingsLoadable = useApiQuery({
    url: '/v2/notification-settings',
    method: 'get',
    options: { keepPreviousData: true },
  });
  const languagesLoadable = useApiQuery({
    url: '/v2/notification-settings/languages',
    method: 'get',
  });
  const mentionsLoadable = useApiQuery({
    url: '/v2/notification-settings/mentions',
    method: 'get',
  });
  const settingMutation = useApiMutation({
    url: '/v2/notification-settings',
    method: 'put',
  });
  const frequencyMutation = useApiMutation({
    url: '/v2/notification-settings/digest-frequency',
    method: 'put',
  });
  const languagesMutation = useApiMutation({
    url: '/v2/notification-settings/languages/{projectId}',
    method: 'put',
  });
  const mentionsMutation = useApiMutation({
    url: '/v2/notification-settings/mentions/{projectId}',
    method: 'put',
  });

  const settings = settingsLoadable.data;

  if (!settings) {
    return null;
  }

  const afterChange = () => settingsLoadable.refetch();

  const localizationCopy: Partial<
    Record<LocalizationType, { label: string; description: string }>
  > = {
    KEYS_ADDED: {
      label: t('settings_notifications_type_keys_added', 'New keys'),
      description: t(
        'settings_notifications_type_keys_added_description',
        'New keys waiting for translation'
      ),
    },
    IMPORTED: {
      label: t('settings_notifications_type_imported', 'Imports'),
      description: t(
        'settings_notifications_type_imported_description',
        'Keys and translations uploaded from a file or the CLI'
      ),
    },
    AUTOMATICALLY_TRANSLATED: {
      label: t(
        'settings_notifications_type_auto_translated',
        'Auto-translated'
      ),
      description: t(
        'settings_notifications_type_auto_translated_description',
        'Machine translations waiting for review'
      ),
    },
    SOURCE_CHANGED: {
      label: t(
        'settings_notifications_type_source_changed',
        'Source text changed'
      ),
      description: t(
        'settings_notifications_type_source_changed_description',
        'Source text changed, translations may need an update'
      ),
    },
    STRINGS_TRANSLATED: {
      label: t(
        'settings_notifications_type_strings_translated',
        'Translated by others'
      ),
      description: t(
        'settings_notifications_type_strings_translated_description',
        'Someone else translated a key'
      ),
    },
    STRINGS_REVIEWED: {
      label: t(
        'settings_notifications_type_strings_reviewed',
        'Reviewed by others'
      ),
      description: t(
        'settings_notifications_type_strings_reviewed_description',
        'Someone else reviewed a translation'
      ),
    },
  };

  const localizationItems = (types: LocalizationType[]): Item[] =>
    types.flatMap((type) => {
      const setting = settings.localization.find((s) => s.type === type);
      const copy = localizationCopy[type];
      if (!setting || !copy) {
        return [];
      }
      return [
        {
          key: type,
          group: 'LOCALIZATION' as const,
          type,
          channels: setting,
          ...copy,
        },
      ];
    });

  const workItems: Item[] = [
    {
      key: 'COMMENTS',
      group: 'COMMENTS',
      channels: settings.comments,
      label: t('settings_notifications_mentions_description', 'Mentions'),
      description: settings.comments.email
        ? t(
            'settings_notifications_mentions_subdescription_email',
            'Someone mentions you in a comment. Emailed within a few minutes.'
          )
        : t(
            'settings_notifications_mentions_subdescription',
            'Someone mentions you in a comment'
          ),
    },
    ...localizationItems([
      'KEYS_ADDED',
      'IMPORTED',
      'AUTOMATICALLY_TRANSLATED',
      'SOURCE_CHANGED',
    ]),
    // shortcut: shown without the isEnabled('TASKS') EE check so the prototype has it, restore before shipping
    {
      key: 'TASKS',
      group: 'TASKS' as const,
      channels: settings.tasks,
      label: t('settings_notifications_tasks_description'),
      description: settings.tasks.email
        ? t(
            'settings_notifications_tasks_subdescription_email',
            'Assigned, completed, closed. Emailed within a few minutes.'
          )
        : t('settings_notifications_tasks_subdescription'),
    },
  ];
  const teamItems: Item[] = [
    {
      key: MY_TRANSLATIONS,
      group: 'LOCALIZATION',
      channels: myTranslations,
      label: t('settings_notifications_my_translations', 'My translations'),
      description: t(
        'settings_notifications_my_translations_description',
        'Someone reviewed or changed a translation you wrote'
      ),
    },
    ...localizationItems(['STRINGS_TRANSLATED', 'STRINGS_REVIEWED']),
  ];
  const allItems = [...workItems, ...teamItems];

  const delivery = settings.digestFrequency;
  const emailDisabledReason =
    delivery === 'OFF'
      ? t('settings_notifications_tooltip_summary_off', 'Summary email is off')
      : undefined;

  const saveItems = (
    items: Item[],
    channel: Channel,
    enabled: (item: Item) => boolean
  ) =>
    Promise.all(
      items
        .filter((item) => item.channels[channel] !== enabled(item))
        .map((item) => {
          if (item.key === MY_TRANSLATIONS) {
            // shortcut: no backend type yet, kept in this browser
            const next = { ...item.channels, [channel]: enabled(item) };
            const isDefault =
              next.inApp === MY_TRANSLATIONS_DEFAULT.inApp &&
              next.email === MY_TRANSLATIONS_DEFAULT.email;
            writeJson(STORAGE_KEYS.myTranslations, isDefault ? null : next);
            setMyTranslations(next);
            return Promise.resolve();
          }
          return settingMutation.mutateAsync({
            content: {
              'application/json': {
                group: item.group,
                type: item.type,
                channel: channel === 'inApp' ? 'IN_APP' : 'EMAIL',
                enabled: enabled(item),
              },
            },
          });
        })
    );

  const change = async (items: Item[], channel: Channel, enabled: boolean) => {
    await saveItems(items, channel, () => enabled);
    messageService.success(
      <T keyName="settings_notifications_message_saved" />
    );
    afterChange();
  };

  const mutedProjects = (mentionsLoadable.data ?? []).filter((m) => m.muted);
  const isDefault =
    allItems.every(isDefaultChannels) &&
    delivery === 'DAILY' &&
    mutedProjects.length === 0 &&
    !hasPrototypeOverrides();

  const resetToDefaults = async () => {
    await saveItems(allItems, 'inApp', () => true);
    await saveItems(allItems, 'email', (item) => item.group !== 'LOCALIZATION');
    await frequencyMutation.mutateAsync({
      content: { 'application/json': { frequency: 'DAILY' } },
    });
    await Promise.all([
      ...mutedProjects.map((m) =>
        mentionsMutation.mutateAsync({
          path: { projectId: m.projectId },
          content: { 'application/json': { muted: false } },
        })
      ),
      ...(languagesLoadable.data ?? []).map((p) =>
        languagesMutation.mutateAsync({
          path: { projectId: p.projectId },
          content: { 'application/json': { languageIds: undefined } },
        })
      ),
    ]);
    clearPrototypeStorage();
    messageService.success(
      <T keyName="settings_notifications_message_saved" />
    );
    await Promise.all([
      settingsLoadable.refetch(),
      languagesLoadable.refetch(),
      mentionsLoadable.refetch(),
    ]);
    setProjectsKey((k) => k + 1);
  };

  const confirmReset = () =>
    confirmation({
      title: t(
        'settings_notifications_reset_title',
        'Reset notification settings?'
      ),
      message: t(
        'settings_notifications_reset_message',
        'All notification settings, including custom project settings, will be reset to defaults.'
      ),
      confirmButtonText: t('settings_notifications_reset_confirm', 'Reset'),
      onConfirm: resetToDefaults,
    });

  const renderRows = (items: Item[]) =>
    items.map((item) => (
      <ChannelRow
        key={item.key}
        label={item.label}
        description={item.description}
        inApp={{ checked: item.channels.inApp }}
        email={{ checked: item.channels.email }}
        emailDisabledReason={
          item.group === 'LOCALIZATION' ? emailDisabledReason : undefined
        }
        onChange={(channel, enabled) => change([item], channel, enabled)}
        dataCyType={item.key}
      />
    ));

  const renderPlainSection = (
    title: React.ReactNode,
    dataCy: string,
    columns?: boolean
  ) => (
    <>
      <StyledSectionSpacer />
      <StyledSectionTitle
        gridColumn={columns ? '1 / 2' : '1 / -1'}
        py={1}
        data-cy="notifications-settings-section"
        data-cy-section={dataCy}
      >
        {title}
      </StyledSectionTitle>
      {columns && (
        <>
          <StyledTableHeader>
            {t('settings_notifications_channel_in_app')}
          </StyledTableHeader>
          <StyledTableHeader>
            {t('settings_notifications_channel_email')}
          </StyledTableHeader>
        </>
      )}
    </>
  );

  const renderSection = (
    title: React.ReactNode,
    dataCy: string,
    items: Item[],
    columns?: boolean
  ) => (
    <>
      {renderPlainSection(title, dataCy, columns)}
      {renderRows(items)}
    </>
  );

  return (
    <BaseUserSettingsView
      windowTitle={t('settings_notifications_title')}
      title={t('settings_notifications_title')}
      titleVariant="h4"
      loading={settingsLoadable.isFetching}
      navigation={[
        [
          t('user_menu_notifications'),
          LINKS.USER_ACCOUNT_NOTIFICATIONS.build(),
        ],
      ]}
      hideChildrenOnLoading={false}
      customButtons={[
        <Button
          key="reset"
          color="inherit"
          sx={{
            color: 'text.primary',
            mr: -2,
            visibility: isDefault ? 'hidden' : 'visible',
          }}
          onClick={confirmReset}
          data-cy="notifications-settings-reset"
        >
          {t('settings_notifications_reset', 'Reset to defaults')}
        </Button>,
      ]}
    >
      <StyledRoot>
        {renderSection(
          <Box display="flex" alignItems="center" gap={0.5}>
            {t('settings_notifications_group_my_work', 'My work')}
            <Tooltip
              title={t(
                'settings_notifications_my_work_languages_hint',
                'Covers the languages you translate or review. You can change them for each project below.'
              )}
              disableInteractive
            >
              <Box display="flex" color="text.secondary">
                <InfoCircle width={16} height={16} />
              </Box>
            </Tooltip>
          </Box>,
          'work',
          workItems,
          true
        )}
        {renderSection(
          t('settings_notifications_group_team_activity', 'Team activity'),
          'team-activity',
          teamItems
        )}
        {renderPlainSection(
          t('settings_notifications_group_account', 'Account'),
          'account'
        )}
        <Box data-cy="notifications-settings-security-row">
          <Typography variant="body1">
            {t('settings_notifications_security_title', 'Security alerts')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t(
              'settings_notifications_security_description',
              'Password change, two-factor sign-in'
            )}
          </Typography>
        </Box>
        <StyledAlwaysOn>
          {t('settings_notifications_always_on', 'Always on')}
        </StyledAlwaysOn>
        <StyledAlwaysOn>
          {t('settings_notifications_always_on', 'Always on')}
        </StyledAlwaysOn>
        {renderPlainSection(
          t('settings_notifications_group_delivery', 'Delivery'),
          'delivery'
        )}
        <WorkEmailFrequencyRow value={delivery} afterChange={afterChange} />
        {renderPlainSection(
          t('settings_notifications_group_projects', 'Projects'),
          'projects'
        )}
        <NotificationProjectsSection key={projectsKey} settings={settings} />
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
