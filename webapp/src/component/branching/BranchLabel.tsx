import { InputBaseComponentProps, styled } from '@mui/material';
import React from 'react';
import { GitBranch01 } from '@untitled-ui/icons-react';

const StyledFakeInput = styled('div')`
  box-sizing: content-box;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

export const BranchLabel = React.forwardRef(function BranchLabel(
  { value, placeholder, ...rest }: InputBaseComponentProps,
  ref
) {
  return (
    <StyledFakeInput
      tabIndex={0}
      ref={ref}
      style={{ display: 'flex' }}
      {...(rest as any)}
    >
      <GitBranch01 height={20} width={20} style={{ marginRight: 10 }} />
      <div>{value || placeholder}</div>
    </StyledFakeInput>
  );
});
