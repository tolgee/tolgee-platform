import { useState } from 'react';
import { Box, Button, Chip, styled, Typography } from '@mui/material';
import { Check } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

type ProjectLanguages =
  components['schemas']['ProjectNotificationLanguagesModel'];

const COLLAPSED_COUNT = 6;

const StyledSection = styled(Box)`
  grid-column: 1 / -1;
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledProject = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledChips = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledChipLabel = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.75)};
`;

const ProjectLanguagesRow = ({
  project,
  afterChange,
}: {
  project: ProjectLanguages;
  afterChange: () => void;
}) => {
  const { t } = useTranslate();
  const [expanded, setExpanded] = useState(false);
  const mutation = useApiMutation({
    url: '/v2/notification-settings/languages/{projectId}',
    method: 'put',
  });

  const save = (languageIds: number[] | undefined) =>
    mutation.mutate(
      {
        path: { projectId: project.projectId },
        content: { 'application/json': { languageIds } },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="settings_notifications_message_saved" />
          );
          afterChange();
        },
      }
    );

  const followed = new Set(project.followedLanguageIds);

  const toggle = (languageId: number) => {
    if (followed.has(languageId)) {
      save(project.followedLanguageIds.filter((id) => id !== languageId));
      return;
    }
    save([...project.followedLanguageIds, languageId]);
  };

  const stateText = {
    DEFAULT: t(
      'settings_notifications_languages_state_default',
      'picked from your recent work, until you change it'
    ),
    CHOSEN: t('settings_notifications_languages_state_chosen', 'your choice'),
    MUTED: t(
      'settings_notifications_languages_state_muted',
      'muted, no languages picked'
    ),
  }[project.state];

  const hiddenCount = project.availableLanguages.length - COLLAPSED_COUNT;
  const shownLanguages =
    expanded || hiddenCount <= 0
      ? project.availableLanguages
      : project.availableLanguages.slice(0, COLLAPSED_COUNT);

  return (
    <StyledProject
      data-cy="notifications-settings-languages-project"
      data-cy-project-id={project.projectId}
    >
      <Box>
        <Typography variant="body1">{project.projectName}</Typography>
        <Box display="flex" alignItems="center" gap={1}>
          <Typography
            variant="body2"
            color="text.secondary"
            data-cy="notifications-settings-languages-state"
            data-cy-state={project.state}
          >
            {stateText}
          </Typography>
          {project.state !== 'DEFAULT' && (
            <Button
              size="small"
              variant="text"
              disabled={mutation.isLoading}
              onClick={() => save(undefined)}
              data-cy="notifications-settings-languages-reset"
            >
              {t(
                'settings_notifications_languages_reset',
                'Use my recent work'
              )}
            </Button>
          )}
        </Box>
      </Box>
      <StyledChips>
        {shownLanguages.map((lang) => {
          const isFollowed = followed.has(lang.id);
          return (
            <Chip
              key={lang.id}
              clickable
              disabled={mutation.isLoading}
              color={isFollowed ? 'primary' : 'default'}
              variant={isFollowed ? 'filled' : 'outlined'}
              onClick={() => toggle(lang.id)}
              aria-pressed={isFollowed}
              data-cy="notifications-settings-languages-chip"
              data-cy-language-tag={lang.tag}
              data-cy-followed={isFollowed}
              label={
                <StyledChipLabel>
                  <FlagImage width={16} flagEmoji={lang.flagEmoji || ''} />
                  <span>{lang.name}</span>
                  {isFollowed && <Check width={14} height={14} />}
                </StyledChipLabel>
              }
            />
          );
        })}
        {!expanded && hiddenCount > 0 && (
          <Chip
            clickable
            variant="outlined"
            onClick={() => setExpanded(true)}
            data-cy="notifications-settings-languages-more"
            label={t('settings_notifications_languages_more', '+{count} more', {
              count: hiddenCount,
            })}
          />
        )}
      </StyledChips>
    </StyledProject>
  );
};

export const NotificationLanguagesSection = () => {
  const { t } = useTranslate();
  const projectsLoadable = useApiQuery({
    url: '/v2/notification-settings/languages',
    method: 'get',
    options: { keepPreviousData: true },
  });

  const projects = projectsLoadable.data;
  if (!projects?.length) {
    return null;
  }

  return (
    <StyledSection data-cy="notifications-settings-languages">
      <Box>
        <Typography variant="subtitle1" sx={{ fontWeight: 500, mt: 2 }}>
          {t('settings_notifications_group_languages', 'Languages per project')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t(
            'settings_notifications_languages_hint',
            'You hear about work only in the languages you follow. Mentions always arrive.'
          )}
        </Typography>
      </Box>
      {projects.map((project) => (
        <ProjectLanguagesRow
          key={project.projectId}
          project={project}
          afterChange={() => projectsLoadable.refetch()}
        />
      ))}
      <Typography variant="body2" color="text.secondary">
        {t(
          'settings_notifications_languages_footer',
          'Only languages you can edit or review are shown. New keys, auto-translated and source changes use these languages. Tick a language to follow it.'
        )}
      </Typography>
    </StyledSection>
  );
};
