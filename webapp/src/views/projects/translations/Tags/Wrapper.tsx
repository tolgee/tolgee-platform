import { styled } from '@mui/material';
import clsx from 'clsx';

import { stopBubble } from 'tg.fixtures/eventHandler';

const StyledWrapper = styled('div')`
  display: flex;
  outline: 0;
  cursor: default;
  padding: 4px 4px;
  border-radius: 12px;
  align-items: center;
  height: 24px;
  font-size: 14px;
  background: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[100]
      : theme.palette.emphasis[200]};
  border: 1px solid transparent;
  min-width: 24px;
  box-sizing: border-box;

  & input {
    color: ${({ theme }) => theme.palette.text.primary};
  }

  &.preview {
    background: ${({ theme }) => theme.palette.background.default};
    border: 1px solid ${({ theme }) => theme.palette.text.secondary};
    color: ${({ theme }) => theme.palette.text.secondary};
  }

  &.hover {
    &:focus-within,
    &:hover {
      border: 1px solid ${({ theme }) => theme.palette.primary.main};
      color: ${({ theme }) => theme.palette.primary.main};
    }
  }

  &.input {
    padding: 4px 6px;
  }

  &.clickable {
    cursor: pointer;
  }

  &.tagAdd {
    width: 24px;
    &.fullLabel {
      padding: 0 6px;
      width: initial;
    }
    justify-content: center;
  }
`;

type Props = {
  role?: 'input' | 'add';
  onClick?: () => void;
  className?: string;
};

export const Wrapper: React.FC<Props> = ({
  children,
  role,
  onClick,
  className,
}) => {
  switch (role) {
    case 'add':
      return (
        <StyledWrapper
          as="button"
          data-cy="translations-tag-add"
          className={clsx('preview', 'clickable', 'hover', 'tagAdd', className)}
          onClick={stopBubble(onClick)}
        >
          {children}
        </StyledWrapper>
      );
    case 'input':
      return (
        <StyledWrapper
          data-cy="translations-tag-input"
          className={clsx('preview', 'hover', 'input', className)}
          onClick={stopBubble(onClick)}
        >
          {children}
        </StyledWrapper>
      );
    default:
      return (
        <StyledWrapper
          data-cy="translations-tag"
          className={clsx({
            hover: Boolean(onClick),
            clickable: Boolean(onClick),
            [className || '']: true,
          })}
          onClick={stopBubble(onClick)}
        >
          {children}
        </StyledWrapper>
      );
  }
};
