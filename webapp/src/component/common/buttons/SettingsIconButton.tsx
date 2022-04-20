import IconButton, { IconButtonProps } from '@mui/material/IconButton';
import SettingsIcon from '@mui/icons-material/Settings';

export function SettingsIconButton(props: IconButtonProps) {
  return (
    <IconButton aria-label="settings" size="large" {...props}>
      <SettingsIcon />
    </IconButton>
  );
}
