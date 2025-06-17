import { IconButton, IconButtonOwnProps, Tooltip } from '@mui/material';

type Props = {
  onClick?: () => void;
  icon: React.ElementType<{ width: number; height: number }>;
  color?: IconButtonOwnProps['color'];
  tooltip: string;
  disabled?: boolean;
};

export const SuggestionAction = ({
  icon,
  onClick,
  color,
  tooltip,
  disabled,
}: Props) => {
  const Icon = icon;

  const button = (
    <IconButton
      onClick={onClick}
      size="small"
      color={color}
      sx={{ margin: '-4px' }}
      disabled={disabled}
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
};
