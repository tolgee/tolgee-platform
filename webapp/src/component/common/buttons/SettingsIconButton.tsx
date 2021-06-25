import IconButton, { IconButtonProps } from '@material-ui/core/IconButton';
import SettingsIcon from '@material-ui/icons/Settings';

export function SettingsIconButton(props: IconButtonProps) {
  return (
    <IconButton aria-label="settings" {...props}>
      <SettingsIcon />
    </IconButton>
  );
}
