import { FilterList } from '@mui/icons-material';
import { Box, Button, styled, useMediaQuery } from '@mui/material';
import { T } from '@tolgee/react';

import { ActivityTitle } from 'tg.component/activity/ActivityTitle';
import { buildActivity } from 'tg.component/activity/activityTools';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { UserName } from 'tg.component/common/UserName';

import { useActivityFilter } from './useActivityFilter';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

const StyledContainer = styled('div')`
  margin-top: -4px;
  margin-bottom: 12px;
  background: ${({ theme }) => theme.palette.revisionFilterBanner.background};
  padding: 0px 4px 0px 14px;
  border-radius: 4px;
  height: 40px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  max-width: 100%;
  align-items: center;
`;

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

const StyledLabel = styled('div')`
  color: ${({ theme }) => theme.palette.revisionFilterBanner.highlightText};
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 16px;
  flex-shrink: 1;
  overflow: hidden;
`;

const StyledLabelText = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
`;

const StyledTime = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
`;

const StyledClear = styled('div')`
  flex-grow: 1;
  display: flex;
  justify-content: flex-end;
  white-space: nowrap;
`;

type Props = {
  revisionId: number;
};

export const RevisionFilterIndicator = ({ revisionId }: Props) => {
  const project = useProject();

  const { clear } = useActivityFilter();

  const { data } = useApiQuery({
    url: '/v2/projects/{projectId}/activity/revisions/{revisionId}',
    method: 'get',
    path: { projectId: project.id, revisionId: revisionId },
  });

  const activity = data && buildActivity(data);
  const lang = useCurrentLanguage();
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isSmall = useMediaQuery(
    `@media(max-width: ${rightPanelWidth + 1000}px)`
  );
  if (!activity) {
    return null;
  }

  const date = new Date(data.timestamp);

  return (
    <StyledContainer>
      <StyledLabel>
        <FilterList color="inherit" />
        <StyledLabelText>
          <T keyName="activity_filter_indicator_label" />
        </StyledLabelText>
      </StyledLabel>
      {!isSmall && (
        <StyledDescription>
          {data.author && (
            <Box gridArea="avatar" sx={{ marginRight: '6px' }}>
              <AvatarImg
                size={24}
                owner={{
                  type: 'USER',
                  id: data.author.id,
                  avatar: data.author.avatar,
                  deleted: data.author.deleted,
                }}
              />
            </Box>
          )}
          <StyledUser>
            {data.author?.deleted ? (
              <UserName {...data.author} />
            ) : (
              data.author?.name
            )}
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
      )}
      <StyledClear>
        <Button size="small" onClick={clear} color="inherit">
          <T keyName="activity_filter_indicator_clear" />
        </Button>
      </StyledClear>
    </StyledContainer>
  );
};
