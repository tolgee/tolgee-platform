import { Popover } from '@mui/material';
import React, { ComponentProps, FC } from 'react';
import { USAGE_ELEMENT_ID } from '../../component/CriticalUsageCircle';

export type PlanLimitPopoverWrapperProps = {
  open: boolean;
  onClose: () => void;
  popoverProps?: Omit<ComponentProps<typeof Popover>, 'open'>;
};

export const PlanLimitPopoverWrapper: FC<PlanLimitPopoverWrapperProps> = ({
  open,
  onClose,
  children,
  popoverProps,
  ...props
}) => {
  const anchorEl = document.getElementById(USAGE_ELEMENT_ID);

  return (
    <Popover
      open={open}
      onClose={onClose}
      anchorEl={open ? anchorEl : undefined}
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-description"
      {...(anchorEl
        ? {
            anchorOrigin: { horizontal: 'right', vertical: 'bottom' },
            transformOrigin: { horizontal: 'right', vertical: 'top' },
          }
        : {
            anchorOrigin: { horizontal: 'center', vertical: 'top' },
            transformOrigin: { horizontal: 'center', vertical: 'center' },
          })}
      data-cy={props['data-cy']}
      {...popoverProps}
    >
      {children}
    </Popover>
  );
};
