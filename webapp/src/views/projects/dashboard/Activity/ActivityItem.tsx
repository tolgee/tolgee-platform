import { Box, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useCurrentLanguage } from '@tolgee/react';
import { getActivityLabel } from 'tg.component/activity/getActivityName';
import { Activity } from 'tg.component/activity/Activity';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto 100px 1fr;
  gap: 2px 5px;
  grid-template-areas:
    'avatar user actionName'
    'avatar time actionBody';
`;

const StyledTime = styled('div')`
  display: flex;
  font-size: 11px;
`;

type Props = {
  item: ProjectActivityModel;
};

export const ActivityItem: React.FC<Props> = ({ item }) => {
  const date = new Date(item.timestamp);
  const isToday = date.toLocaleDateString() === new Date().toLocaleDateString();
  const lang = useCurrentLanguage();

  return (
    <StyledContainer>
      {item.author && (
        <Box gridArea="avatar" justifySelf="start">
          <AvatarImg
            size={24}
            owner={{
              type: 'USER',
              id: item.author.id,
              avatar: item.author.avatar,
            }}
            autoAvatarType="IDENTICON"
            circle
          />
        </Box>
      )}
      <Box gridArea="user">{item.author?.name}</Box>
      <Box gridArea="actionName">{getActivityLabel({ data: item })}</Box>
      <Box gridArea="time">
        <StyledTime>
          {!isToday && date.toLocaleDateString(lang()) + ' '}
          {date.toLocaleTimeString(lang(), {
            hour: 'numeric',
            minute: 'numeric',
          })}
        </StyledTime>
      </Box>
      <Box gridArea="actionBody">
        <Activity data={item} />
      </Box>
    </StyledContainer>
  );
};
