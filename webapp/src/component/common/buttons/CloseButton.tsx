import { FC, MouseEvent, ReactNode } from 'react';
import { IconButton, styled, Tooltip } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import clsx from 'clsx';

const CloseButtonWrapperContainer = styled('div')<{ xs?: boolean }>`
  position: relative;
  width: min-content;
  min-width: 0;

  & .closeButton {
    position: absolute;
    z-index: 2;
    top: -8px;
    right: -8px;
    width: 24px;
    height: 24px;
    background-color: ${({ theme }) =>
      theme.palette.tokens.icon.backgroundDark};
    color: ${({ theme }) => theme.palette.tokens.icon.onDark};
    transition: visibility 0.1s linear, opacity 0.1s linear;
    display: grid;
    align-content: center;
    justify-content: center;
    opacity: 0;

    &.xs {
      width: 20px;
      height: 20px;
    }
  }

  &:hover .closeButton,
  &:focus-within .closeButton {
    opacity: 1;
  }

  & .closeButton:hover {
    background-color: ${({ theme }) =>
      theme.palette.tokens.icon.backgroundDarkHover};
    color: ${({ theme }) => theme.palette.tokens.icon.onDarkHover};
    visibility: visible;
  }
`;

type Props = {
  onClose?: (e: MouseEvent) => void;
  tooltip?: string | JSX.Element;
  className?: string;
  xs?: boolean;
  children: ReactNode; // For wrapped children components
  'data-cy'?: string;
};

export const CloseButton: FC<Props> = ({
  onClose,
  tooltip,
  xs,
  className,
  children,
  'data-cy': dataCy,
}) => {
  if (!onClose) return <>{children}</>;
  return (
    <CloseButtonWrapperContainer>
      <Tooltip title={tooltip || ''} disableInteractive>
        <IconButton
          className={clsx('closeButton', className, xs && 'xs')}
          onClick={onClose}
          data-cy={dataCy}
        >
          <XClose width={xs ? 16 : 20} height={xs ? 16 : 20} />
        </IconButton>
      </Tooltip>
      {children}
    </CloseButtonWrapperContainer>
  );
};
