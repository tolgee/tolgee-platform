import { Box, styled, SxProps } from '@mui/material';
import React from 'react';

export const PlanTitleText = styled(Box)`
  font-size: 24px;
`;

type Props = {
  title: string | React.ReactNode;
  sx?: SxProps;
  className?: string;
};

export const PlanTitle: React.FC<Props> = ({ title, sx, className }) => {
  return <PlanTitleText {...{ sx, className }}>{title}</PlanTitleText>;
};
