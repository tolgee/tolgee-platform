import { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
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

const TEAM_TYPES = ['STRINGS_TRANSLATED', 'STRINGS_REVIEWED'];

type ProjectPref = { custom?: boolean; muted?: boolean };
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
    all[projectId] = pref;
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

const StyledDialogGrid = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 80px 110px;
  row-gap: 12px;
  align-items: center;
`;

const StyledSectionTitle = styled(Box)`
  font-size: 15px;
  font-weight: 500;
  text-transform: uppercase;
  padding-top: ${({ theme }) => theme.spacing(2)};
`;

const StyledColumnHeader = styled(Box)`
  justify-self: end;
  width: 34px;
  display: flex;
  justify-content: center;
  white-space: nowrap;
  font-size: 13px;
  padding-top: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.text.secondary};
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

type TypeRow = {
  type: string;
  label: string;
  channels: Channels;
};

const useTypeRows = (settings: Settings): TypeRow[] => {
  const { t } = useTranslate();
  const labels: Record<string, string> = {
    KEYS_ADDED: t('settings_notifications_type_keys_added', 'New keys'),
    IMPORTED: t('settings_notifications_type_imported', 'Imports'),
    AUTOMATICALLY_TRANSLATED: t(
      'settings_notifications_type_auto_translated',
      'Auto-translated'
    ),
    SOURCE_CHANGED: t(
      'settings_notifications_type_source_changed',
      'Source text changed'
    ),
    STRINGS_TRANSLATED: t(
      'settings_notifications_type_strings_translated',
      'Translated by others'
    ),
    STRINGS_REVIEWED: t(
      'settings_notifications_type_strings_reviewed',
      'Reviewed by others'
    ),
  };
  const order = Object.keys(labels);
  const rows: TypeRow[] = settings.localization
    .filter((s) => labels[s.type])
    .sort((a, b) => order.indexOf(a.type) - order.indexOf(b.type))
    .map((s) => ({ type: s.type, label: labels[s.type], channels: s }));
  // shortcut: no isEnabled('TASKS') EE check in the prototype, restore before shipping
  return [
    ...rows,
    {
      type: 'TASKS',
      label: t('settings_notifications_tasks_description'),
      channels: settings.tasks,
    },
  ];
};

const countChannels = (rows: TypeRow[], overrides: Overrides = {}) =>
  rows.reduce(
    (acc, row) => {
      const effective = { ...row.channels, ...overrides[row.type] };
      return {
        inApp: acc.inApp + (effective.inApp ? 1 : 0),
        email: acc.email + (effective.email ? 1 : 0),
      };
    },
    { inApp: 0, email: 0 }
  );

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
  const [muted, setMuted] = useState(Boolean(pref.muted));
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

  const isCustom =
    !!pref.custom ||
    !!pref.muted ||
    mentionsMuted ||
    Object.keys(initialOverrides).length > 0;

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
      <DialogTitle sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
        <Box flex={1}>
          {project.projectName}
          <Typography
            variant="body2"
            color="text.secondary"
            data-cy="notifications-project-dialog-inherited-banner"
          >
            {isCustom
              ? t(
                  'settings_notifications_project_dialog_custom',
                  'Custom settings'
                )
              : t(
                  'settings_notifications_project_dialog_inherited',
                  'Uses your global settings'
                )}
          </Typography>
        </Box>
        <FormControlLabel
          labelPlacement="start"
          sx={{ mr: '-12px' }}
          control={
            <Switch
              checked={muted}
              onChange={(e) => setMuted(e.target.checked)}
              data-cy="notifications-project-dialog-mute"
            />
          }
          label={
            <Typography variant="body1">
              {t('settings_notifications_project_mute', 'Mute this project')}
            </Typography>
          }
        />
      </DialogTitle>
      <DialogContent>
        <StyledDialogGrid
          sx={{
            opacity: muted ? 0.4 : 1,
            pointerEvents: muted ? 'none' : undefined,
          }}
        >
          <StyledSectionTitle gridColumn="1 / -1">
            {t('settings_notifications_languages_title', 'Languages')}
          </StyledSectionTitle>
          <Box
            gridColumn="1 / -1"
            display="flex"
            flexWrap="wrap"
            alignItems="center"
            columnGap={4}
            rowGap={1}
          >
            <Box flex="1 1 200px">
              <Typography variant="body1">
                {t(
                  'settings_notifications_project_languages_label',
                  'Notify me about'
                )}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t(
                  'settings_notifications_languages_scope_hint',
                  'Mentions and tasks assigned to you arrive in any language.'
                )}
              </Typography>
            </Box>
            <Box flex="0 0 auto" ml="auto">
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

          <StyledSectionTitle>
            {t('settings_notifications_group_my_work', 'My work')}
          </StyledSectionTitle>
          <StyledColumnHeader>
            {t('settings_notifications_channel_in_app')}
          </StyledColumnHeader>
          <StyledColumnHeader>
            {t('settings_notifications_channel_email')}
          </StyledColumnHeader>
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
          {typeRows
            .filter((row) => !TEAM_TYPES.includes(row.type))
            .map(renderTypeRow)}

          <StyledSectionTitle gridColumn="1 / -1">
            {t('settings_notifications_group_team_activity', 'Team activity')}
          </StyledSectionTitle>
          {typeRows
            .filter((row) => TEAM_TYPES.includes(row.type))
            .map(renderTypeRow)}
        </StyledDialogGrid>
      </DialogContent>
      <Box display="flex" justifyContent="space-between">
        <DialogActions>
          {isCustom && (
            <Button
              sx={{ color: 'text.primary' }}
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
                'Reset to global'
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
                muted,
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
  const hasOverrides = Object.keys(overrides).length > 0;
  const counts = countChannels(typeRows, overrides);

  const summary = () => {
    if (pref.muted) {
      return t('settings_notifications_project_muted', 'Muted');
    }
    const parts = [
      pref.custom &&
        t('settings_notifications_project_custom', 'Custom languages'),
      hasOverrides &&
        t(
          'settings_notifications_project_counts',
          'In-App: {inApp} · Email: {email}',
          counts
        ),
      mentionsMuted &&
        t('settings_notifications_project_mentions_off', 'Mentions off'),
    ].filter(Boolean);
    return parts.length
      ? parts.join(' · ')
      : t('settings_notifications_project_same_as_global', 'Same as global');
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
      <Box className={TABLE_LAST_CELL}>
        <IconButton
          size="small"
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
  globalTags: string[] | null;
  afterChange: (tags: string[] | null) => void;
};

const AllProjectsRow = ({
  projects,
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
          {t(
            'settings_notifications_global_all_projects',
            'Global (all projects)'
          )}
        </Typography>
      </div>
      <StyledSummary data-cy="notifications-all-projects-languages">
        <Flags languages={selected} />
      </StyledSummary>
      <StyledSummary>
        {globalTags === null
          ? t('settings_notifications_languages_all_short', 'All languages')
          : t('settings_notifications_project_custom', 'Custom languages')}
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
            {t(
              'settings_notifications_global_all_projects',
              'Global (all projects)'
            )}
            <Typography variant="body2" color="text.secondary">
              {t(
                'settings_notifications_all_projects_subtitle',
                'Applies to projects without their own languages'
              )}
            </Typography>
          </DialogTitle>
          <DialogContent>
            <Box
              display="flex"
              flexWrap="wrap"
              alignItems="center"
              columnGap={4}
              rowGap={1}
            >
              <Box flex="1 1 200px">
                <Typography variant="body1">
                  {t(
                    'settings_notifications_project_languages_label',
                    'Notify me about'
                  )}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {t(
                    'settings_notifications_all_projects_hint',
                    'Projects without these languages notify you about none.'
                  )}
                </Typography>
              </Box>
              <Box flex="0 0 auto" ml="auto">
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
            typeRows={typeRows}
            afterChange={refetch}
          />
        ))}
      </StyledLanguageTable>
    </Box>
  );
};
