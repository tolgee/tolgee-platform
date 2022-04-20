import { FunctionComponent, ReactNode } from 'react';
import { Button, ButtonProps, styled } from '@mui/material';

const StyledButton = styled(Button)`
  border: 1px solid ${({ theme }) => theme.palette.emphasis['200']};
  border-radius: 50px;
  padding: ${({ theme }) => theme.spacing(0.125, 1.5)};
  background-color: ${({ theme }) => theme.palette.background.default};
  cursor: pointer;
  min-width: 0px;
`;

const StyledIconWrapper = styled('div')`
  display: inline-flex;
  align-items: center;
  margin-right: ${({ theme }) => theme.spacing(0.5)};
  & svg {
    font-size: 16px;
  }
`;

export const ChipButton: FunctionComponent<
  {
    beforeIcon?: ReactNode;
    onClick: () => void;
  } & ButtonProps
> = (props) => {
  const { beforeIcon, children, ...buttonProps } = props;

  return (
    <StyledButton {...buttonProps}>
      {beforeIcon && <StyledIconWrapper>{beforeIcon}</StyledIconWrapper>}
      {children}
    </StyledButton>
  );
};
