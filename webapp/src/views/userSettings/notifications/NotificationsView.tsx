import React, { useState } from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  styled,
  Switch,
  Tooltip,
  Typography,
} from '@mui/material';
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
import {
  StyledColumnHeader,
  StyledSectionTitle,
} from 'tg.views/userSettings/notifications/notificationsTableStyles';

const StyledRoot = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 72px 72px;
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

const StyledSectionSpacer = styled(Box)`
  grid-column: 1 / -1;
  height: ${({ theme }) => theme.spacing(3)};
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
  hint?: string;
  alsoTypes?: LocalizationType[];
};

// shortcut: no backend types yet, kept in this browser
const LOCAL_ITEMS: Record<string, string> = {
  SUGGESTED_BY_OTHERS: STORAGE_KEYS.suggestedByOthers,
  COMMENTED_BY_OTHERS: STORAGE_KEYS.commentedByOthers,
};
const LOCAL_DEFAULTS: Record<string, Channels> = {
  SUGGESTED_BY_OTHERS: { inApp: false, email: false },
  COMMENTED_BY_OTHERS: { inApp: false, email: false },
};

const readLocal = () =>
  Object.fromEntries(
    Object.entries(LOCAL_ITEMS).map(([key, storageKey]) => [
      key,
      readJson<Channels>(storageKey, LOCAL_DEFAULTS[key]),
    ])
  ) as Record<string, Channels>;

const TEAM_TYPES: string[] = ['STRINGS_TRANSLATED', 'STRINGS_REVIEWED'];

const defaultChannels = (item: Item): Channels =>
  LOCAL_DEFAULTS[item.key] ?? {
    inApp: !TEAM_TYPES.includes(item.key),
    email: !TEAM_TYPES.includes(item.key),
  };

const isDefaultChannels = (item: Item) =>
  item.channels.inApp === defaultChannels(item).inApp &&
  item.channels.email === defaultChannels(item).email;

export const NotificationsView: React.FC<
  React.PropsWithChildren<unknown>
> = () => {
  const { t } = useTranslate();
  const [projectsKey, setProjectsKey] = useState(0);
  const [localChannels, setLocalChannels] = useState(readLocal);

  const projectsLoadable = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: { size: 1000 },
  });
  const suggestionsEnabled = (
    projectsLoadable.data?._embedded?.projects ?? []
  ).some((p) => p.suggestionsMode !== 'DISABLED');

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
    Record<
      LocalizationType,
      {
        label: string;
        description: string;
        hint?: string;
        alsoTypes?: LocalizationType[];
      }
    >
  > = {
    KEYS_ADDED: {
      label: t('settings_notifications_type_keys_added', 'New keys'),
      description: t(
        'settings_notifications_type_keys_added_description',
        'Waiting for translation, including imports'
      ),
      alsoTypes: ['IMPORTED'],
    },
    AUTOMATICALLY_TRANSLATED: {
      label: t(
        'settings_notifications_type_auto_translated',
        'Machine translated'
      ),
      description: t(
        'settings_notifications_type_auto_translated_description',
        'Auto or bulk translated (AI, TM or other providers)'
      ),
    },
    SOURCE_CHANGED: {
      label: t(
        'settings_notifications_type_source_changed',
        'Source text changed'
      ),
      description: t(
        'settings_notifications_type_source_changed_description',
        'Base language texts edited, translations may need an update'
      ),
    },
    STRINGS_TRANSLATED: {
      label: t(
        'settings_notifications_type_strings_translated',
        'Translated by others'
      ),
      description: t(
        'settings_notifications_type_strings_translated_description',
        'Someone else translated keys'
      ),
    },
    STRINGS_REVIEWED: {
      label: t(
        'settings_notifications_type_strings_reviewed',
        'Reviewed by others'
      ),
      description: t(
        'settings_notifications_type_strings_reviewed_description',
        'Someone else reviewed translations'
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

  const directItems: Item[] = [
    {
      key: 'COMMENTS',
      group: 'COMMENTS',
      channels: settings.comments,
      label: t('settings_notifications_mentions_description', 'Mentions'),
      description: t(
        'settings_notifications_mentions_subdescription',
        'Someone mentioned you in comments'
      ),
    },
    // shortcut: shown without the isEnabled('TASKS') EE check so the prototype has it, restore before shipping
    {
      key: 'TASKS',
      group: 'TASKS' as const,
      channels: settings.tasks,
      label: t('settings_notifications_tasks_description'),
      description: t('settings_notifications_tasks_subdescription'),
    },
  ];
  const workItems: Item[] = [
    ...localizationItems([
      'KEYS_ADDED',
      'AUTOMATICALLY_TRANSLATED',
      'SOURCE_CHANGED',
    ]),
  ];
  const teamItems: Item[] = [
    ...localizationItems(['STRINGS_TRANSLATED', 'STRINGS_REVIEWED']),
    ...(suggestionsEnabled
      ? [
          {
            key: 'SUGGESTED_BY_OTHERS',
            group: 'LOCALIZATION' as const,
            channels: localChannels.SUGGESTED_BY_OTHERS,
            label: t(
              'settings_notifications_suggested_by_others',
              'Suggested by others'
            ),
            description: t(
              'settings_notifications_suggested_by_others_description',
              'Someone else suggested translations'
            ),
          },
        ]
      : []),
    {
      key: 'COMMENTED_BY_OTHERS',
      group: 'LOCALIZATION' as const,
      channels: localChannels.COMMENTED_BY_OTHERS,
      label: t(
        'settings_notifications_commented_by_others',
        'Commented by others'
      ),
      description: t(
        'settings_notifications_commented_by_others_description',
        'Someone else commented on translations'
      ),
    },
  ];
  const updateItems = [...workItems, ...teamItems];
  const allItems = [...directItems, ...updateItems];

  const delivery = settings.digestFrequency;

  const saveItems = (
    items: Item[],
    channel: Channel,
    enabled: (item: Item) => boolean
  ) =>
    Promise.all(
      items
        .filter((item) => item.channels[channel] !== enabled(item))
        .map((item) => {
          const storageKey = LOCAL_ITEMS[item.key];
          if (storageKey) {
            const next = { ...item.channels, [channel]: enabled(item) };
            const isDefault =
              next.inApp === LOCAL_DEFAULTS[item.key].inApp &&
              next.email === LOCAL_DEFAULTS[item.key].email;
            writeJson(storageKey, isDefault ? null : next);
            setLocalChannels((prev) => ({ ...prev, [item.key]: next }));
            return Promise.resolve();
          }
          return Promise.all(
            [item.type, ...(item.alsoTypes ?? [])].map((type) =>
              settingMutation.mutateAsync({
                content: {
                  'application/json': {
                    group: item.group,
                    type,
                    channel: channel === 'inApp' ? 'IN_APP' : 'EMAIL',
                    enabled: enabled(item),
                  },
                },
              })
            )
          );
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
    await saveItems(allItems, 'inApp', (item) => defaultChannels(item).inApp);
    await saveItems(allItems, 'email', (item) => defaultChannels(item).email);
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
    setLocalChannels(readLocal());
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
        hint={item.hint}
        description={item.description}
        inApp={{ checked: item.channels.inApp }}
        email={{ checked: item.channels.email }}
        onChange={(channel, enabled) => change([item], channel, enabled)}
        dataCyType={item.key}
      />
    ));

  const renderUpdateRows = (items: Item[]) =>
    items.map((item) => (
      <ChannelRow
        key={item.key}
        label={item.label}
        description={item.description}
        inApp={{ checked: item.channels.inApp }}
        email={{ checked: item.channels.email }}
        emailDisabledReason={
          delivery === 'OFF'
            ? t(
                'settings_notifications_tooltip_summary_off',
                'Email summary is off'
              )
            : undefined
        }
        onChange={(channel, enabled) => change([item], channel, enabled)}
        dataCyType={item.key}
      />
    ));

  const renderPlainSection = (
    title: React.ReactNode,
    dataCy: string,
    columns?: 'instant' | 'summary'
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
          <StyledColumnHeader>
            {t('settings_notifications_channel_in_app')}
          </StyledColumnHeader>
          <StyledColumnHeader>
            {columns === 'summary' ? (
              <T
                keyName="settings_notifications_channel_summary_email"
                defaultValue="Summary<br></br>email"
                params={{ br: <br /> }}
              />
            ) : (
              t('settings_notifications_channel_email')
            )}
          </StyledColumnHeader>
        </>
      )}
    </>
  );

  const renderSection = (
    title: React.ReactNode,
    dataCy: string,
    items: Item[],
    columns?: 'instant' | 'summary'
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
        {renderSection('', 'direct', directItems, 'instant')}
        {renderPlainSection(
          <Box display="flex" alignItems="center" gap={0.5}>
            {t('settings_notifications_group_my_work', 'Work for you')}
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
          'summary'
        )}
        {renderUpdateRows(workItems)}
        {renderPlainSection(
          t('settings_notifications_group_team_activity', 'Team activity'),
          'team-activity',
          'summary'
        )}
        {renderUpdateRows(teamItems)}
        {renderPlainSection(
          t('settings_notifications_group_summary_email', 'Summary email'),
          'summary-email'
        )}
        <WorkEmailFrequencyRow value={delivery} afterChange={afterChange} />
        {renderPlainSection(
          t('settings_notifications_group_account', 'Account'),
          'account',
          'instant'
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
        {(['IN_APP', 'EMAIL'] as const).map((channel) => (
          <StyledAlwaysOn key={channel}>
            <Tooltip
              title={t('settings_notifications_always_on', 'Always on')}
              disableInteractive
            >
              <span>
                <Switch
                  sx={{ mr: '-12px' }}
                  checked
                  disabled
                  data-cy="notifications-settings-security-toggle"
                  data-cy-channel={channel}
                />
              </span>
            </Tooltip>
          </StyledAlwaysOn>
        ))}
        {Boolean(languagesLoadable.data?.length) && (
          <>
            {renderPlainSection(
              t('settings_notifications_group_projects', 'Projects'),
              'projects'
            )}
            <NotificationProjectsSection
              key={projectsKey}
              settings={settings}
            />
          </>
        )}
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
