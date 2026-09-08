import React from 'react';
import { Typography, styled } from '@mui/material';

const StyledField = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.25)};
`;

/** A caption-style label above a value — the one grammar for labeled values in the apps UI. */
export const AppField = ({
  label,
  children,
}: {
  label: React.ReactNode;
  children: React.ReactNode;
}) => (
  <StyledField>
    <Typography variant="caption" color="text.secondary">
      {label}
    </Typography>
    {children}
  </StyledField>
);
