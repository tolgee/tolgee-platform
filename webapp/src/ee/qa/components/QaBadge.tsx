import { Box, styled, Typography } from '@mui/material';
import React from 'react';

import { QaBadgeProps } from '../../../eeSetup/EeModuleType';
import { QaCheck } from 'tg.component/CustomIcons';

const StyledQaBadge = styled(Box)`
  display: inline-flex;
  align-items: start;
  margin: ${({ theme }) => theme.spacing(1)};
`;

const StyledQaBadgeText = styled(Typography)`
  margin-top: -8px;
  margin-left: -10px;
  border-radius: 10px;
  min-height: 20px;
  min-width: 20px;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  background-color: ${({ theme }) => theme.palette.primary.main};
  color: ${({ theme }) => theme.palette.primary.contrastText};
`;

const StyledQaIcon = styled(QaCheck)`
  width: 24px;
  height: 24px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

export const QaBadge = ({
  count,
  ...props
}: QaBadgeProps) => {
  if (!count || count === 0) {
    return null;
  }

  return (
    <StyledQaBadge {...props}>
      <StyledQaIcon />
      <StyledQaBadgeText>{count > 9 ? '9+' : count}</StyledQaBadgeText>
    </StyledQaBadge>
  );
};
