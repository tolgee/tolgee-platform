import { InputLabel, styled, Typography } from '@mui/material';
import React from 'react';

const StyledInputLabel = styled(InputLabel)`
  font-size: 14px;
  margin-bottom: 5px;
  font-weight: 500px;
`;

export const StyledError = styled(Typography)`
  display: flex;
  min-height: 1.2rem;
`;

export const FieldLabel: React.FC = ({ children }) => {
  return (
    <StyledInputLabel data-cy="translation-field-label">
      {children}
    </StyledInputLabel>
  );
};

export const FieldError: React.FC<{ error: React.ReactNode }> = ({ error }) => {
  return (
    <StyledError variant="caption" color="error">
      {error}
    </StyledError>
  );
};
