import * as React from 'react';
import { FC, ReactNode } from 'react';
import { Box, styled, Typography } from '@mui/material';

export interface SubscriptionRowPlanInfoProps {
  label: ReactNode;
  children: ReactNode;
  dataCy?: string;
}

const StyledContainer = styled(Box)`
  display: inline-block;
  margin-left: 4px;
`;

export const SubscriptionRowPlanInfo: FC<SubscriptionRowPlanInfoProps> = ({
  label,
  children,
  dataCy,
}) => {
  return (
    <StyledContainer data-cy={dataCy}>
      <Typography sx={{ fontSize: '10px' }}>{label}</Typography>
      {children}
    </StyledContainer>
  );
};
