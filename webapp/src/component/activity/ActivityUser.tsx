import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { ActivityModel } from './types';
import { UserName } from '../common/UserName';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto 1fr;
  gap: 2px 5px;
  align-items: start;
  grid-template-areas:
    'avatar user'
    'avatar time';
`;

const StyledAvatar = styled(Box)`
  margin-top: 8px;
`;

const StyledTime = styled('div')`
  display: flex;
  font-size: 11px;
`;

const StyledUser = styled(Box)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledApp = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: 6px;
`;

const StyledAppBadge = styled('span')`
  font-size: 11px;
  line-height: 1;
  padding: 2px 5px;
  border-radius: 3px;
  color: ${({ theme }) => theme.palette.text.secondary};
  background: ${({ theme }) => theme.palette.tokens.background.selected};
`;

type Props = {
  item: ActivityModel;
  onlyTime?: boolean;
};

export const ActivityUser: React.FC<React.PropsWithChildren<Props>> = ({
  item,
  onlyTime,
}) => {
  const date = new Date(item.timestamp);
  const lang = useCurrentLanguage();

  return (
    <StyledContainer>
      {item.author && (
        <StyledAvatar gridArea="avatar">
          <AvatarImg
            size={24}
            owner={{
              type: 'USER',
              id: item.author.id,
              avatar: item.author.avatar,
              deleted: item.author.deleted,
            }}
          />
        </StyledAvatar>
      )}
      <StyledUser gridArea="user">
        {item.author?.deleted ? (
          <UserName {...item.author} />
        ) : (
          item.author?.name
        )}
        {!item.author && item.app && (
          <StyledApp data-cy="activity-app" data-cy-app-id={item.app.appId}>
            {item.app.name ?? item.app.appId}
            <StyledAppBadge>
              <T keyName="activity_author_app_badge" defaultValue="App" />
            </StyledAppBadge>
          </StyledApp>
        )}
      </StyledUser>
      <Box gridArea="time">
        <StyledTime>
          {!onlyTime && date.toLocaleDateString(lang) + ' '}
          {date.toLocaleTimeString(lang, {
            hour: 'numeric',
            minute: 'numeric',
          })}
        </StyledTime>
      </Box>
    </StyledContainer>
  );
};
