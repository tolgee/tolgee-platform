import { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  styled,
  Switch,
  Tooltip,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Settings01 } from '@untitled-ui/icons-react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import {
  StyledLanguageTable,
  TABLE_DIVIDER,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
} from 'tg.component/languages/tableStyles';
import { ChannelRow } from 'tg.views/userSettings/notifications/ChannelRow';
import {
  StyledColumnHeader,
  StyledSectionTitle,
} from 'tg.views/userSettings/notifications/notificationsTableStyles';
import {
  projectOverridesKey,
  readJson,
  STORAGE_KEYS,
  writeJson,
} from 'tg.views/userSettings/notifications/prototypeStorage';

type Settings = components['schemas']['NotificationSettingModel'];
type ProjectLanguages =
  components['schemas']['ProjectNotificationLanguagesModel'];
type Language = components['schemas']['NotificationLanguageModel'];
type Channels = { inApp: boolean; email: boolean };

const TEAM_TYPES = [
  'STRINGS_TRANSLATED',
  'STRINGS_REVIEWED',
  'SUGGESTED_BY_OTHERS',
  'COMMENTED_BY_OTHERS',
];

type ProjectPref = {
  custom?: boolean;
  muted?: boolean;
  mentionsMutedBefore?: boolean;
};
type Overrides = Record<string, Partial<Channels>>;

const readOverrides = (projectId: number): Overrides =>
  readJson<Overrides>(projectOverridesKey(projectId), {});

const writeOverrides = (projectId: number, overrides: Overrides) =>
  writeJson(
    projectOverridesKey(projectId),
    Object.keys(overrides).length ? overrides : null
  );

export const readPrefs = () =>
  readJson<Record<number, ProjectPref>>(STORAGE_KEYS.projectLanguages, {});

const writePref = (projectId: number, pref: ProjectPref) => {
  const all = readPrefs();
  delete all[projectId];
  if (pref.custom || pref.muted) {
    all[projectId] = {
      custom: pref.custom,
      muted: pref.muted,
      mentionsMutedBefore: pref.muted ? pref.mentionsMutedBefore : undefined,
    };
  }
  writeJson(
    STORAGE_KEYS.projectLanguages,
    Object.keys(all).length ? all : null
  );
};

const sameIds = (a: number[], b: number[]) =>
  a.length === b.length && a.every((id) => b.includes(id));

const globalIdsFor = (
  project: ProjectLanguages,
  globalTags: string[] | null
): number[] | undefined =>
  globalTags === null
    ? undefined
    : project.availableLanguages
        .filter((l) => globalTags.includes(l.tag))
        .map((l) => l.id);

const followedLanguages = (project: ProjectLanguages) =>
  project.followedLanguageIds.length
    ? project.availableLanguages.filter((l) =>
        project.followedLanguageIds.includes(l.id)
      )
    : project.availableLanguages;

const MAX_FLAGS = 4;

const StyledSummary = styled(Box)`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: ${({ theme }) => theme.spacing(0, 2)};
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledInheritedBanner = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
  color: ${({ theme }) => theme.palette.text.primary};
  font-size: 14px;
  padding: ${({ theme }) => theme.spacing(0.5, 3)};
`;

const StyledDialogGrid = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 72px 72px;
  row-gap: 12px;
  padding-right: 16px;
  align-items: center;
`;

const Flags = ({ languages }: { languages: Language[] }) => {
  const shown = languages.slice(0, MAX_FLAGS);
  const hidden = languages.slice(MAX_FLAGS);
  return (
    <Box display="flex" alignItems="center" gap="6px">
      {shown.map((l) => (
        <FlagImage key={l.id} flagEmoji={l.flagEmoji ?? ''} height={16} />
      ))}
      {hidden.length > 0 && (
        <Tooltip title={languages.map((l) => l.name).join(', ')}>
          <span>+{hidden.length}</span>
        </Tooltip>
      )}
    </Box>
  );
};

const channelCounts = (channels: Channels[]) => ({
  inApp: channels.filter((c) => c.inApp).length,
  email: channels.filter((c) => c.email).length,
});

type TypeRow = {
  type: string;
  label: string;
  description?: string;
  channels: Channels;
};

const useTypeRows = (settings: Settings): TypeRow[] => {
  const { t } = useTranslate();
  const copy: Record<string, { label: string; description: string }> = {
    KEYS_ADDED: {
      label: t('settings_notifications_type_keys_added', 'New keys'),
      description: t(
        'settings_notifications_type_keys_added_description',
        'Waiting for translation, including imports'
      ),
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
  const order = Object.keys(copy);
  const rows: TypeRow[] = settings.localization
    .filter((s) => copy[s.type])
    .sort((a, b) => order.indexOf(a.type) - order.indexOf(b.type))
    .map((s) => ({ type: s.type, ...copy[s.type], channels: s }));
  // shortcut: no isEnabled('TASKS') EE check in the prototype, restore before shipping
  return [
    ...rows,
    {
      type: 'TASKS',
      label: t('settings_notifications_tasks_description'),
      description: t('settings_notifications_tasks_subdescription'),
      channels: settings.tasks,
    },
  ];
};

const useSuggestedRow = (): TypeRow => {
  const { t } = useTranslate();
  // shortcut: no backend type yet, the global value lives in this browser
  return {
    type: 'SUGGESTED_BY_OTHERS',
    label: t(
      'settings_notifications_suggested_by_others',
      'Suggested by others'
    ),
    description: t(
      'settings_notifications_suggested_by_others_description',
      'Someone else suggested translations'
    ),
    channels: readJson<Channels>(STORAGE_KEYS.suggestedByOthers, {
      inApp: false,
      email: false,
    }),
  };
};

const useCommentedRow = (): TypeRow => {
  const { t } = useTranslate();
  // shortcut: no backend type yet, the global value lives in this browser
  return {
    type: 'COMMENTED_BY_OTHERS',
    label: t(
      'settings_notifications_commented_by_others',
      'Commented by others'
    ),
    description: t(
      'settings_notifications_commented_by_others_description',
      'Someone else commented on translations'
    ),
    channels: readJson<Channels>(STORAGE_KEYS.commentedByOthers, {
      inApp: false,
      email: false,
    }),
  };
};

type ProjectDialogProps = {
  project: ProjectLanguages;
  globalTags: string[] | null;
  pref: ProjectPref;
  mentionsMuted: boolean;
  mentionChannels: Channels;
  typeRows: TypeRow[];
  overrides: Overrides;
  onClose: () => void;
  afterChange: () => void;
};

const ProjectDialog = ({
  project,
  globalTags,
  pref,
  mentionsMuted,
  mentionChannels,
  typeRows,
  overrides: initialOverrides,
  onClose,
  afterChange,
}: ProjectDialogProps) => {
  const { t } = useTranslate();
  const [tags, setTags] = useState<string[]>(
    followedLanguages(project).map((l) => l.tag)
  );
  const [mentions, setMentions] = useState(!mentionsMuted || !!pref.muted);
  const [overrides, setOverrides] = useState<Overrides>(initialOverrides);

  const languagesMutation = useApiMutation({
    url: '/v2/notification-settings/languages/{projectId}',
    method: 'put',
  });
  const mentionsMutation = useApiMutation({
    url: '/v2/notification-settings/mentions/{projectId}',
    method: 'put',
  });

  const globalIds =
    globalIdsFor(project, globalTags) ??
    project.availableLanguages.map((l) => l.id);
  const selectedIds = project.availableLanguages
    .filter((l) => tags.includes(l.tag))
    .map((l) => l.id);
  const isCustom =
    !!pref.muted ||
    !sameIds(selectedIds, globalIds) ||
    !mentions ||
    Object.keys(overrides).length > 0;

  const mentionRow: TypeRow = {
    type: 'COMMENTS',
    label: '',
    channels: mentionChannels,
  };

  const effective = (row: TypeRow): Channels => ({
    ...row.channels,
    ...overrides[row.type],
  });

  const setChannel = (row: TypeRow, channel: keyof Channels, on: boolean) => {
    const next = { ...overrides[row.type], [channel]: on };
    if (next[channel] === row.channels[channel]) {
      delete next[channel];
    }
    const updated = { ...overrides, [row.type]: next };
    if (Object.keys(next).length === 0) {
      delete updated[row.type];
    }
    setOverrides(updated);
  };

  const save = async (next: {
    tags: string[] | 'global';
    muted: boolean;
    mentionsOn: boolean;
    overrides: Overrides;
  }) => {
    const fromGlobal = globalIdsFor(project, globalTags);
    const globalIds = fromGlobal ?? project.availableLanguages.map((l) => l.id);
    const ids =
      next.tags === 'global'
        ? globalIds
        : project.availableLanguages
            .filter((l) => (next.tags as string[]).includes(l.tag))
            .map((l) => l.id);
    const custom = !sameIds(ids, globalIds);
    const languageIds = next.muted ? [] : custom ? ids : fromGlobal;
    await languagesMutation.mutateAsync({
      path: { projectId: project.projectId },
      content: { 'application/json': { languageIds } },
    });
    const mentionsOff = next.muted || !next.mentionsOn;
    if (mentionsOff !== mentionsMuted) {
      await mentionsMutation.mutateAsync({
        path: { projectId: project.projectId },
        content: { 'application/json': { muted: mentionsOff } },
      });
    }
    writePref(project.projectId, {
      custom: custom || undefined,
      muted: next.muted || undefined,
    });
    writeOverrides(project.projectId, next.overrides);
    messageService.success(
      <T keyName="settings_notifications_message_saved" />
    );
    afterChange();
    onClose();
  };

  const busy = languagesMutation.isLoading || mentionsMutation.isLoading;

  const renderHeader = (
    title: React.ReactNode,
    email: 'instant' | 'summary'
  ) => (
    <>
      <StyledSectionTitle sx={title ? { pt: 2 } : undefined}>
        {title}
      </StyledSectionTitle>
      <StyledColumnHeader pt={2}>
        {t('settings_notifications_channel_in_app')}
      </StyledColumnHeader>
      <StyledColumnHeader>
        {email === 'summary' ? (
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
  );

  const renderTypeRow = (row: TypeRow) => {
    const value = effective(row);
    return (
      <ChannelRow
        key={row.type}
        label={row.label}
        inApp={{ checked: value.inApp }}
        email={{ checked: value.email }}
        onChange={(channel, on) => setChannel(row, channel, on)}
        dataCyType={row.type}
      />
    );
  };

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      {!isCustom && (
        <StyledInheritedBanner data-cy="notifications-project-dialog-inherited-banner">
          {t(
            'settings_notifications_project_dialog_inherited_banner',
            'Settings inherited from Global settings'
          )}
        </StyledInheritedBanner>
      )}
      <DialogTitle
        sx={{ display: 'flex', alignItems: 'center', gap: 2, pr: 5, pb: 0.5 }}
      >
        <Box flex={1}>
          <div>
            {t(
              'settings_notifications_project_dialog_title',
              'Project notifications'
            )}
          </div>
          <Box sx={{ fontSize: 14, fontWeight: 'normal' }}>
            {project.projectName}
          </Box>
        </Box>
        <Box sx={{ width: 240, fontSize: 16, fontWeight: 'normal' }}>
          <Box
            sx={{
              '& .MuiFormControl-root, & .MuiInputBase-root': {
                width: '100%',
              },
            }}
          >
            <LanguagesSelect
              enableEmpty
              placeholder={t(
                'settings_notifications_languages_none',
                'No languages'
              )}
              languages={project.availableLanguages}
              selectAllToggle
              value={tags}
              onChange={setTags}
              context="notifications-project-languages-picker"
            />
          </Box>
        </Box>
      </DialogTitle>
      <DialogContent>
        <StyledDialogGrid>
          {renderHeader(null, 'instant')}
          <ChannelRow
            label={t('settings_notifications_mentions_description', 'Mentions')}
            inApp={{ checked: mentions }}
            email={{ checked: effective(mentionRow).email }}
            onChange={(channel, on) =>
              channel === 'inApp'
                ? setMentions(on)
                : setChannel(mentionRow, 'email', on)
            }
            dataCyType="COMMENTS"
          />
          {typeRows.filter((row) => row.type === 'TASKS').map(renderTypeRow)}

          {renderHeader(
            t('settings_notifications_group_my_work', 'Work for you'),
            'summary'
          )}
          {typeRows
            .filter(
              (row) => !TEAM_TYPES.includes(row.type) && row.type !== 'TASKS'
            )
            .map(renderTypeRow)}

          {renderHeader(
            t('settings_notifications_group_team_activity', 'Team activity'),
            'summary'
          )}
          {typeRows
            .filter((row) => TEAM_TYPES.includes(row.type))
            .map(renderTypeRow)}
        </StyledDialogGrid>
      </DialogContent>
      <Box display="flex" justifyContent="space-between">
        <DialogActions>
          {isCustom && (
            <Button
              variant="outlined"
              color="secondary"
              disabled={busy}
              onClick={() =>
                save({
                  tags: 'global',
                  muted: false,
                  mentionsOn: true,
                  overrides: {},
                })
              }
              data-cy="notifications-project-dialog-reset"
            >
              {t(
                'settings_notifications_project_dialog_reset',
                'Reset to Global'
              )}
            </Button>
          )}
        </DialogActions>
        <DialogActions>
          <Button onClick={onClose}>
            {t('settings_notifications_project_dialog_cancel', 'Cancel')}
          </Button>
          <Button
            variant="contained"
            color="primary"
            disabled={busy}
            onClick={() =>
              save({
                tags,
                muted: false,
                mentionsOn: mentions,
                overrides,
              })
            }
            data-cy="notifications-project-dialog-save"
          >
            {t('settings_notifications_project_dialog_save', 'Save')}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
};

type RowProps = {
  project: ProjectLanguages;
  globalTags: string[] | null;
  mentionsMuted: boolean;
  mentionChannels: Channels;
  typeRows: TypeRow[];
  afterChange: () => void;
};

const ProjectRow = ({
  project,
  globalTags,
  mentionsMuted,
  mentionChannels,
  typeRows,
  afterChange,
}: RowProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const [overrides, setOverrides] = useState(() =>
    readOverrides(project.projectId)
  );
  const [pref, setPref] = useState<ProjectPref>(
    () => readPrefs()[project.projectId] ?? {}
  );
  const mentionsMutation = useApiMutation({
    url: '/v2/notification-settings/mentions/{projectId}',
    method: 'put',
  });

  // shortcut: only mentions mute on the backend, the rest of the project mute lives in this browser
  const toggleMute = async () => {
    const muted = !pref.muted;
    const mentionsOff = muted ? true : !!pref.mentionsMutedBefore;
    if (mentionsOff !== mentionsMuted) {
      await mentionsMutation.mutateAsync({
        path: { projectId: project.projectId },
        content: { 'application/json': { muted: mentionsOff } },
      });
    }
    const next = {
      ...pref,
      muted: muted || undefined,
      mentionsMutedBefore: muted ? mentionsMuted : undefined,
    };
    writePref(project.projectId, next);
    setPref(next);
    afterChange();
  };

  const summary = () => {
    if (pref.muted) {
      return t('settings_notifications_project_muted', 'Muted');
    }
    if (
      !pref.custom &&
      !pref.muted &&
      !mentionsMuted &&
      Object.keys(overrides).length === 0
    ) {
      return t('settings_notifications_project_global', 'Global settings');
    }
    const channels = pref.muted
      ? []
      : [
          ...typeRows.map((row) => ({
            ...row.channels,
            ...overrides[row.type],
          })),
          ...(mentionsMuted ? [] : [mentionChannels]),
        ];
    return t(
      'settings_notifications_project_counts',
      'In-App {inApp} · Email {email}',
      channelCounts(channels)
    );
  };

  return (
    <>
      <div className={TABLE_DIVIDER} />
      <div className={TABLE_FIRST_CELL}>
        <Typography variant="body1">{project.projectName}</Typography>
      </div>
      <StyledSummary
        data-cy="notifications-project-languages"
        sx={{ opacity: pref.muted ? 0.4 : 1 }}
      >
        <Flags languages={followedLanguages(project)} />
      </StyledSummary>
      <StyledSummary data-cy="notifications-project-summary">
        {summary()}
      </StyledSummary>
      <Box
        className={TABLE_LAST_CELL}
        display="flex"
        alignItems="center"
        gap={2}
      >
        <Box
          component="label"
          display="flex"
          alignItems="center"
          sx={{
            fontSize: 14,
            color: 'text.secondary',
            cursor: 'pointer',
            gap: 0.5,
          }}
        >
          <Switch
            size="small"
            checked={!!pref.muted}
            disabled={mentionsMutation.isLoading}
            onChange={toggleMute}
            data-cy="notifications-project-mute-switch"
            data-cy-project-id={project.projectId}
          />
          {t('settings_notifications_project_mute_label', 'Mute')}
        </Box>
        <IconButton
          size="small"
          disabled={!!pref.muted}
          onClick={() => setOpen(true)}
          data-cy="notifications-project-settings-button"
          data-cy-project-id={project.projectId}
        >
          <Settings01 />
        </IconButton>
      </Box>
      {open && (
        <ProjectDialog
          project={project}
          globalTags={globalTags}
          pref={pref}
          mentionsMuted={mentionsMuted}
          mentionChannels={mentionChannels}
          typeRows={typeRows}
          overrides={overrides}
          onClose={() => {
            setOverrides(readOverrides(project.projectId));
            setPref(readPrefs()[project.projectId] ?? {});
            setOpen(false);
          }}
          afterChange={afterChange}
        />
      )}
    </>
  );
};

type AllProjectsRowProps = {
  projects: ProjectLanguages[];
  typeRows: TypeRow[];
  mentionChannels: Channels;
  globalTags: string[] | null;
  afterChange: (tags: string[] | null) => void;
};

const AllProjectsRow = ({
  projects,
  typeRows,
  mentionChannels,
  globalTags,
  afterChange,
}: AllProjectsRowProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const [tags, setTags] = useState<string[]>([]);
  const prefs = readPrefs();
  const languagesMutation = useApiMutation({
    url: '/v2/notification-settings/languages/{projectId}',
    method: 'put',
  });
  const allLanguages = Object.values(
    Object.fromEntries(
      projects.flatMap((p) => p.availableLanguages).map((l) => [l.tag, l])
    )
  ).sort((a, b) => a.name.localeCompare(b.name));
  const selected =
    globalTags === null
      ? allLanguages
      : allLanguages.filter((l) => globalTags.includes(l.tag));

  const save = async () => {
    const next = tags.length === allLanguages.length ? null : tags;
    writeJson(STORAGE_KEYS.globalLanguages, next);
    await Promise.all(
      projects
        .filter(
          (p) => !prefs[p.projectId]?.custom && !prefs[p.projectId]?.muted
        )
        .map((p) =>
          languagesMutation.mutateAsync({
            path: { projectId: p.projectId },
            content: {
              'application/json': { languageIds: globalIdsFor(p, next) },
            },
          })
        )
    );
    messageService.success(
      <T keyName="settings_notifications_message_saved" />
    );
    setOpen(false);
    afterChange(next);
  };

  return (
    <>
      <div className={TABLE_FIRST_CELL}>
        <Typography variant="body1" fontWeight={500}>
          {t('settings_notifications_global_all_projects', 'Global settings')}
        </Typography>
      </div>
      <StyledSummary data-cy="notifications-all-projects-languages">
        <Flags languages={selected} />
      </StyledSummary>
      <StyledSummary data-cy="notifications-all-projects-summary">
        {t(
          'settings_notifications_project_counts',
          'In-App {inApp} · Email {email}',
          channelCounts([...typeRows.map((r) => r.channels), mentionChannels])
        )}
      </StyledSummary>
      <Box className={TABLE_LAST_CELL}>
        <IconButton
          size="small"
          onClick={() => {
            setTags(selected.map((l) => l.tag));
            setOpen(true);
          }}
          data-cy="notifications-all-projects-settings-button"
        >
          <Settings01 />
        </IconButton>
      </Box>
      {open && (
        <Dialog open onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>
            {t('settings_notifications_global_all_projects', 'Global settings')}
            <Typography variant="body2" color="text.secondary">
              {t(
                'settings_notifications_all_projects_languages_subtitle',
                'Languages used in all projects, unless a project has its own.'
              )}
            </Typography>
          </DialogTitle>
          <DialogContent>
            <Box
              sx={{
                '& .MuiFormControl-root, & .MuiInputBase-root': {
                  width: '100%',
                  maxWidth: 320,
                },
              }}
            >
              <LanguagesSelect
                enableEmpty
                placeholder={t(
                  'settings_notifications_languages_none',
                  'No languages'
                )}
                languages={allLanguages}
                selectAllToggle
                value={tags}
                onChange={setTags}
                context="notifications-all-projects-languages-picker"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>
              {t('settings_notifications_project_dialog_cancel', 'Cancel')}
            </Button>
            <Button
              variant="contained"
              color="primary"
              disabled={languagesMutation.isLoading}
              onClick={save}
              data-cy="notifications-all-projects-save"
            >
              {t('settings_notifications_project_dialog_save', 'Save')}
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </>
  );
};

export const NotificationProjectsSection = ({
  settings,
}: {
  settings: Settings;
}) => {
  const typeRows = useTypeRows(settings);
  const suggestedRow = useSuggestedRow();
  const commentedRow = useCommentedRow();
  const allProjectsLoadable = useApiQuery({
    url: '/v2/projects',
    method: 'get',
    query: { size: 1000 },
  });
  const suggestionProjects = new Set(
    (allProjectsLoadable.data?._embedded?.projects ?? [])
      .filter((p) => p.suggestionsMode !== 'DISABLED')
      .map((p) => p.id)
  );
  const [globalTags, setGlobalTags] = useState<string[] | null>(() =>
    readJson<string[] | null>(STORAGE_KEYS.globalLanguages, null)
  );
  const languagesLoadable = useApiQuery({
    url: '/v2/notification-settings/languages',
    method: 'get',
    options: { keepPreviousData: true },
  });
  const mentionsLoadable = useApiQuery({
    url: '/v2/notification-settings/mentions',
    method: 'get',
    options: { keepPreviousData: true },
  });

  const projects = languagesLoadable.data;
  if (!projects?.length) {
    return null;
  }
  const mutedMentions = new Set(
    (mentionsLoadable.data ?? []).filter((m) => m.muted).map((m) => m.projectId)
  );
  const refetch = () => {
    languagesLoadable.refetch();
    mentionsLoadable.refetch();
  };

  return (
    <Box gridColumn="1 / -1" data-cy="notifications-projects-table">
      <StyledLanguageTable style={{ gridTemplateColumns: '1fr 1fr 1fr auto' }}>
        <AllProjectsRow
          projects={projects}
          typeRows={[...typeRows, commentedRow]}
          mentionChannels={settings.comments}
          globalTags={globalTags}
          afterChange={(next) => {
            setGlobalTags(next);
            refetch();
          }}
        />
        {projects.map((project) => (
          <ProjectRow
            key={project.projectId}
            project={project}
            globalTags={globalTags}
            mentionsMuted={mutedMentions.has(project.projectId)}
            mentionChannels={settings.comments}
            typeRows={
              suggestionProjects.has(project.projectId)
                ? [...typeRows, suggestedRow, commentedRow]
                : [...typeRows, commentedRow]
            }
            afterChange={refetch}
          />
        ))}
      </StyledLanguageTable>
    </Box>
  );
};
