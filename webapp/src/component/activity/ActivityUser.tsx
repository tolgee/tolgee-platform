import { Box, styled } from '@mui/material';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { ActivityModel } from './types';
import { UserName } from '../common/UserName';
import { TruncatedTooltip } from 'tg.component/activity/TruncatedTooltip';

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
      <TruncatedTooltip gridArea="user" tooltip={item.author?.name}>
        {item.author?.deleted ? (
          <UserName {...item.author} />
        ) : (
          item.author?.name
        )}
      </TruncatedTooltip>
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
