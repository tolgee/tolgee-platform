import { Box, styled, SxProps } from '@mui/material';
import React from 'react';

export const Container = styled(Box)`
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  font-size: 15px;
`;

type Props = {
  children: React.ReactNode;
  sx?: SxProps;
  className?: string;
};

export const TaskId = ({ children, sx, className }: Props) => {
  return <Container {...{ sx, className }}>#{children}</Container>;
};
