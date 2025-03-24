import { InputLabel, styled, SxProps, Typography } from '@mui/material';
import React from 'react';

const StyledInputLabel = styled(InputLabel)`
  font-size: 14px;
  margin-bottom: 5px;
  font-weight: 500px;
`;

export const StyledError = styled(Typography)`
  display: flex;
  min-height: 1.25rem;
`;

type FieldLabelProps = {
  sx?: SxProps;
  className?: string;
};

export const FieldLabel: React.FC<FieldLabelProps> = ({
  children,
  sx,
  className,
}) => {
  return (
    <StyledInputLabel data-cy="translation-field-label" {...{ sx, className }}>
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
