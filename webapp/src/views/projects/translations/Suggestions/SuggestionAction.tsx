import { IconButton, IconButtonOwnProps, Tooltip } from '@mui/material';
import React from 'react';

type Props = {
  onClick?: () => void;
  icon: React.ElementType<{ width: number; height: number }>;
  color?: IconButtonOwnProps['color'];
  tooltip: string;
  disabled?: boolean;
};

export const SuggestionAction = React.forwardRef(function SuggestionAction(
  { icon, onClick, color, tooltip, disabled }: Props,
  ref: React.ForwardedRef<HTMLButtonElement>
) {
  const Icon = icon;

  const button = (
    <IconButton
      onClick={onClick}
      size="small"
      color={color}
      sx={{ margin: '-4px' }}
      disabled={disabled}
      ref={ref}
    >
      <Icon width={20} height={20} />
    </IconButton>
  );

  if (disabled) {
    return button;
  } else {
    return (
      <Tooltip title={tooltip} disableInteractive>
        {button}
      </Tooltip>
    );
  }
});
