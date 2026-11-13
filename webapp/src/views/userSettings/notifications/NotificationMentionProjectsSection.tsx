import { Box, styled, Switch, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

type ProjectMentionSetting =
  components['schemas']['ProjectMentionSettingModel'];

const StyledSection = styled(Box)`
  grid-column: 1 / -1;
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledRow = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 240px;
  align-items: center;
`;

const StyledIndented = styled(Typography)`
  padding-left: ${({ theme }) => theme.spacing(2)};
`;

const ProjectMentionRow = ({
  project,
  afterChange,
}: {
  project: ProjectMentionSetting;
  afterChange: () => void;
}) => {
  const mutation = useApiMutation({
    url: '/v2/notification-settings/mentions/{projectId}',
    method: 'put',
  });

  const save = (muted: boolean) =>
    mutation.mutate(
      {
        path: { projectId: project.projectId },
        content: { 'application/json': { muted } },
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

  return (
    <StyledRow
      data-cy="notifications-settings-mention-project"
      data-cy-project-id={project.projectId}
    >
      <StyledIndented variant="body2">{project.projectName}</StyledIndented>
      <Box textAlign="center">
        <Switch
          checked={!project.muted}
          disabled={mutation.isLoading}
          onChange={(e) => save(!e.target.checked)}
          data-cy="notifications-settings-mention-project-toggle"
          data-cy-muted={project.muted}
        />
      </Box>
    </StyledRow>
  );
};

export const NotificationMentionProjectsSection = () => {
  const { t } = useTranslate();
  const projectsLoadable = useApiQuery({
    url: '/v2/notification-settings/mentions',
    method: 'get',
    options: { keepPreviousData: true },
  });

  const projects = projectsLoadable.data;
  if (!projects?.length) {
    return null;
  }

  return (
    <StyledSection data-cy="notifications-settings-mention-projects">
      <StyledIndented variant="body2" color="text.secondary">
        {t(
          'settings_notifications_mention_projects_hint',
          'Mentions per project. Turn a project off to stop its mentions, for example a public project where you once left a comment.'
        )}
      </StyledIndented>
      {projects.map((project) => (
        <ProjectMentionRow
          key={project.projectId}
          project={project}
          afterChange={() => projectsLoadable.refetch()}
        />
      ))}
    </StyledSection>
  );
};
