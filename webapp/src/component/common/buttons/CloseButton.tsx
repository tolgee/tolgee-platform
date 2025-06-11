import { FC, MouseEvent, ReactNode } from 'react';
import { IconButton, styled, Tooltip } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import clsx from 'clsx';

const CloseButtonWrapperContainer = styled('div')<{ xs?: boolean }>(
  ({ theme, xs }) => ({
    position: 'relative',

    '& .closeButton': {
      position: 'absolute',
      zIndex: 2,
      top: -8,
      right: -8,
      width: xs ? 20 : 24,
      height: xs ? 20 : 24,
      backgroundColor: theme.palette.tokens.icon.backgroundDark,
      color: theme.palette.tokens.icon.onDark,
      transition: 'visibility 0.1s linear, opacity 0.1s linear',
      display: 'grid',
      alignContent: 'center',
      justifyContent: 'center',
      opacity: 0,
    },

    '&:hover .closeButton, &:focus-within .closeButton': {
      opacity: 1,
    },

    '& .closeButton:hover': {
      backgroundColor: theme.palette.tokens.icon.backgroundDarkHover,
      color: theme.palette.tokens.icon.onDarkHover,
      visibility: 'visible',
    },
  })
);

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
    <CloseButtonWrapperContainer xs>
      <Tooltip title={tooltip || ''} disableInteractive>
        <IconButton
          className={clsx('closeButton', className)}
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
