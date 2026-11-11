import { Box, styled } from '@mui/material';

import { ActorAvatar, ActorName } from 'tg.component/activity/SystemActor';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { ActivityModel } from './types';

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

type Props = {
  item: ActivityModel;
  onlyTime?: boolean;
  fullActorLabel?: boolean;
};

export const ActivityUser: React.FC<React.PropsWithChildren<Props>> = ({
  item,
  onlyTime,
  fullActorLabel,
}) => {
  const date = new Date(item.timestamp);
  const lang = useCurrentLanguage();

  const systemActor = item.systemActor;

  return (
    <StyledContainer>
      {(systemActor || item.author) && (
        <StyledAvatar gridArea="avatar">
          <ActorAvatar systemActor={systemActor} author={item.author} />
        </StyledAvatar>
      )}
      <StyledUser gridArea="user">
        <ActorName
          systemActor={systemActor}
          author={item.author}
          full={fullActorLabel}
        />
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
