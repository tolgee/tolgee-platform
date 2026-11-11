import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { ActivityTitle } from 'tg.component/activity/ActivityTitle';
import { buildActivity } from 'tg.component/activity/activityTools';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ActorAvatar, ActorName } from 'tg.component/activity/SystemActor';

import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { PrefilterContainer } from './ContainerPrefilter';

const StyledDescription = styled('div')`
  display: flex;
  align-items: center;
  overflow: hidden;
  max-width: 100%;
`;

const StyledUser = styled(Box)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
`;

const StyledTime = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
`;

type Props = {
  revisionId: number;
};

export const PrefilterActivity = ({ revisionId }: Props) => {
  const project = useProject();

  const { data } = useApiQuery({
    url: '/v2/projects/{projectId}/activity/revisions/{revisionId}',
    method: 'get',
    path: { projectId: project.id, revisionId: revisionId },
    query: {},
  });

  const activity = data && buildActivity(data);
  const lang = useCurrentLanguage();

  if (!activity) {
    return null;
  }

  const date = new Date(data.timestamp);
  const systemActor = data.systemActor;

  return (
    <PrefilterContainer
      title={<T keyName="activity_filter_indicator_label" />}
      content={
        <StyledDescription>
          {(systemActor || data.author) && (
            <Box
              gridArea="avatar"
              sx={{
                marginRight: '6px',
                display: systemActor ? 'flex' : undefined,
              }}
            >
              <ActorAvatar systemActor={systemActor} author={data.author} />
            </Box>
          )}
          <StyledUser>
            <ActorName systemActor={systemActor} author={data.author} />
          </StyledUser>
          <Box sx={{ marginLeft: '12px' }}>
            <ActivityTitle activity={activity} />
          </Box>
          <StyledTime sx={{ marginLeft: '12px' }}>
            {date.toLocaleDateString(lang) + ' '}
            {date.toLocaleTimeString(lang, {
              hour: 'numeric',
              minute: 'numeric',
            })}
          </StyledTime>
        </StyledDescription>
      }
    />
  );
};
