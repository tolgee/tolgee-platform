import React from 'react';
import { Box, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from '../common/avatar/AvatarImg';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledOrgItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: 6px;
  align-items: center;
  text: ${({ theme }) => theme.palette.primaryText};
`;

type Props = {
  data: OrganizationModel;
  size?: number;
};

export const OrganizationItem: React.FC<Props> = ({ data, size = 24 }) => {
  return (
    <StyledOrgItem>
      <Box>
        <AvatarImg
          key={0}
          owner={{
            name: data.name,
            avatar: data.avatar,
            type: 'ORG',
            id: data.id,
          }}
          size={size}
        />
      </Box>
      <Box>{data.name}</Box>
    </StyledOrgItem>
  );
};
