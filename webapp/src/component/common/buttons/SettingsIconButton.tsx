import IconButton, { IconButtonProps } from '@mui/material/IconButton';
import { Settings01 } from '@untitled-ui/icons-react';

export function SettingsIconButton(props: IconButtonProps) {
  return (
    <IconButton aria-label="settings" size="large" {...props}>
      <Settings01 />
    </IconButton>
  );
}
