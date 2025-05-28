import React from 'react';
import { Box, InputLabel, styled, SxProps } from '@mui/material';

const StyledInputLabel = styled(InputLabel)`
  font-size: 16px;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.text.primary};
`;

type FieldLabelProps = {
  sx?: SxProps;
  className?: string;
  rightContent?: React.ReactNode;
};

export const Label: React.FC<FieldLabelProps> = ({
  children,
  sx,
  className,
  rightContent,
}) => {
  return (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems="center"
      mb={1}
    >
      <StyledInputLabel
        data-cy="translation-field-label"
        {...{ sx, className }}
      >
        {children}
      </StyledInputLabel>
      {rightContent}
    </Box>
  );
};
