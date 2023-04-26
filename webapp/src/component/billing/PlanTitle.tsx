import { styled, Typography } from '@mui/material';
import React from 'react';

export const PlanTitleArea = styled('div')`
  grid-area: title;
  height: 40px;
`;

export const PlanTitleText = ({ children }) => (
  <Typography variant="h4" data-cy="billing-plan-title">
    {children}
  </Typography>
);

type Props = {
  title: string | React.ReactNode;
};

export const PlanTitle: React.FC<Props> = ({ title }) => {
  return (
    <PlanTitleArea>
      <PlanTitleText>{title}</PlanTitleText>
    </PlanTitleArea>
  );
};
