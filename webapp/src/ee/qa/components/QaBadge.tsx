import { Badge, Box, styled, Typography } from '@mui/material';
import React from 'react';

import { QaBadgeProps } from '../../../eeSetup/EeModuleType';
import { QaCheck } from 'tg.component/CustomIcons';
import { Check } from '@untitled-ui/icons-react';

const StyledBadge = styled(Badge)`
  display: flex;

  & .unresolved {
    font-size: 10px;
    height: unset;
    padding: 3px 3px;
  }
  & .resolved {
    background: ${({ theme }) => theme.palette.emphasis[600]};
    padding: 0px;
    height: 16px;
    width: 18px;
    min-width: unset;
    align-items: center;
    justify-content: center;
  }
`;

const StyledCheckIcon = styled(Check)`
  color: ${({ theme }) => theme.palette.emphasis[100]};
  width: 14px !important;
  height: 14px !important;
  margin: -5px;
`;

export const QaBadge = ({ count }: QaBadgeProps) => {
  // TODO: "loading" version when count is undefined

  if (!count || count === 0) {
    return (
      <StyledBadge
        badgeContent={<StyledCheckIcon />}
        classes={{
          badge: 'resolved',
        }}
      >
        <QaCheck />
      </StyledBadge>
    );
  }

  return (
    <StyledBadge
      badgeContent={count}
      color="primary"
      classes={{ badge: 'unresolved' }}
    >
      <QaCheck />
    </StyledBadge>
  );
};
