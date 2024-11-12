import { InputBaseComponentProps, styled } from '@mui/material';
import React from 'react';

const StyledPlaceholder = styled('span')`
  color: ${({ theme }) => theme.palette.tokens.text.tertiary};
`;

const StyledFakeInput = styled('div')`
  padding: 8.5px 14px;
  height: 23px;
  box-sizing: content-box;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

export const FakeInput = React.forwardRef(function FakeInput(
  { value, placeholder, ...rest }: InputBaseComponentProps,
  ref
) {
  return (
    <StyledFakeInput tabIndex={0} ref={ref} {...(rest as any)}>
      {value || <StyledPlaceholder>{placeholder}</StyledPlaceholder>}
    </StyledFakeInput>
  );
});
