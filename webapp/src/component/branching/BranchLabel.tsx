import { InputBaseComponentProps } from '@mui/material';
import React from 'react';
import { BranchLabelInput } from 'tg.component/branching/BranchLabelInput';

export const BranchLabel = React.forwardRef(function BranchLabel(
  { value, placeholder, ...rest }: InputBaseComponentProps,
  ref
) {
  return (
    <BranchLabelInput
      value={value}
      placeholder={placeholder}
      tabIndex={0}
      ref={ref}
      style={{ display: 'flex' }}
      {...(rest as any)}
    />
  );
});
