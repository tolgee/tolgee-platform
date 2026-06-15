import { Checkbox as MUICheckbox } from '@mui/material';
import React from 'react';

export type CheckboxProps = React.ComponentProps<typeof MUICheckbox>;

export const Checkbox = React.forwardRef<HTMLButtonElement, CheckboxProps>(
  function Checkbox(props, ref) {
    return <MUICheckbox ref={ref} checked={!!props.value} {...props} />;
  }
);
