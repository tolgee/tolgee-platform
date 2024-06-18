import { SxProps, styled } from '@mui/material';
import React from 'react';

export const PlanTitleText = styled('div')`
  font-size: 24px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

type Props = {
  title: string | React.ReactNode;
  sx?: SxProps;
  className?: string;
};

export const PlanTitle: React.FC<Props> = ({ title, sx, className }) => {
  return <PlanTitleText {...{ sx, className }}>{title}</PlanTitleText>;
};
