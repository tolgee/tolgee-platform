import { FunctionComponent } from 'react';
import {
  InputLabel,
  Switch as MUISwitch,
  styled,
  FormControlLabel,
} from '@mui/material';
import React from 'react';

export const StyledContainer = styled('div')`
  display: grid;
  label {
    margin-bottom: 4px;
  }
`;

export const StyledInputLabel = styled(InputLabel)`
  font-size: 12px;
  font-weight: 400px;
`;

export type SwitchProps = React.ComponentProps<typeof MUISwitch> & {
  label?: React.ReactNode;
  helperText?: React.ReactNode;
};

export const Switch: FunctionComponent<SwitchProps> = React.forwardRef(
  function Switch(props, ref) {
    const { label, helperText, sx, ...otherProps } = props;
    return (
      <StyledContainer>
        <FormControlLabel
          control={<MUISwitch ref={ref} sx={{ ...sx }} {...otherProps} />}
          label={label}
        />
        {helperText && <StyledInputLabel>{helperText}</StyledInputLabel>}
      </StyledContainer>
    );
  }
);
