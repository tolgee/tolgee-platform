import { FunctionComponent } from 'react';
import { InputLabel, TextField as MUITextField, styled } from '@mui/material';
import React from 'react';

export const StyledContainer = styled('div')`
  display: grid;
  label {
    margin-bottom: 4px;
  }
`;

export const StyledInputLabel = styled(InputLabel)`
  font-size: 14px;
  font-weight: 500px;
`;

export type TextFieldProps = React.ComponentProps<typeof MUITextField> & {
  minHeight?: boolean;
};

export const TextField: FunctionComponent<TextFieldProps> = React.forwardRef(
  function TextField(props, ref) {
    const { label, minHeight = true, sx, ...otherProps } = props;
    return (
      <StyledContainer>
        {label && <StyledInputLabel>{label}</StyledInputLabel>}
        <MUITextField
          ref={ref}
          variant="outlined"
          size="small"
          sx={{ minHeight: minHeight ? '64px' : undefined, ...sx }}
          {...otherProps}
        />
      </StyledContainer>
    );
  }
);
