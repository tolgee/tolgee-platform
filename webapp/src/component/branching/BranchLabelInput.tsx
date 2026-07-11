import { InputBaseComponentProps, styled } from '@mui/material';
import React from 'react';
import { GitBranch02 } from '@untitled-ui/icons-react';

const StyledFakeInput = styled('div')`
  box-sizing: content-box;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

export const BranchLabelInput = ({
  value,
  placeholder,
  ...rest
}: {
  value: string;
  placeholder: string;
  rest?: InputBaseComponentProps;
}) => {
  return (
    <StyledFakeInput style={{ display: 'flex' }} {...(rest as any)}>
      <GitBranch02 height={20} width={20} style={{ marginRight: 10 }} />
      <div>{value || placeholder}</div>
    </StyledFakeInput>
  );
};
