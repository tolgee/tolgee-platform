import IconButton, { IconButtonProps } from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/Delete';

export function DeleteIconButton(props: IconButtonProps) {
  return (
    <IconButton aria-label="delete" color="secondary" size="large" {...props}>
      <DeleteIcon />
    </IconButton>
  );
}
