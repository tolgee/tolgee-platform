import React from 'react';
import { Box, styled } from '@mui/material';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { Project } from './types';

const StyledOrgItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: 6px;
  align-items: center;
  text: ${({ theme }) => theme.palette.primaryText};
`;

type Props = {
  data: Project;
  size?: number;
};

export const ProjectSearchSelectItem: React.FC<Props> = ({
  data,
  size = 24,
}) => {
  return (
    <StyledOrgItem>
      <Box>
        <AvatarImg
          key={0}
          owner={{
            name: data.name,
            avatar: data.avatar,
            type: 'PROJECT',
            id: data.id,
          }}
          size={size}
        />
      </Box>
      <Box>{data.name}</Box>
    </StyledOrgItem>
  );
};
