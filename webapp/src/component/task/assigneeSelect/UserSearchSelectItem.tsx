import React from 'react';
import { Box, styled } from '@mui/material';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { User } from './types';

const StyledOrgItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: 6px;
  align-items: center;
  text: ${({ theme }) => theme.palette.primaryText};
`;

type Props = {
  data: User;
  size?: number;
};

export const UserSearchSelectItem: React.FC<Props> = ({ data, size = 24 }) => {
  return (
    <StyledOrgItem>
      <Box>
        <AvatarImg
          key={0}
          owner={{
            name: data.name,
            avatar: data.avatar,
            type: 'USER',
            id: data.id,
          }}
          size={size}
        />
      </Box>
      <Box>{data.name}</Box>
    </StyledOrgItem>
  );
};
