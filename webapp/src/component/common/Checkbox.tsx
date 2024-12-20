import { FunctionComponent } from 'react';
import { Checkbox as MUICheckbox } from '@mui/material';
import React from 'react';

export type CheckboxProps = React.ComponentProps<typeof MUICheckbox>;

export const Checkbox: FunctionComponent<CheckboxProps> = React.forwardRef(
  function Checkbox(props, ref) {
    return <MUICheckbox ref={ref} checked={!!props.value} {...props} />;
  }
);
