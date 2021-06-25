import IconButton, { IconButtonProps } from '@material-ui/core/IconButton';
import DeleteIcon from '@material-ui/icons/Delete';

export function DeleteIconButton(props: IconButtonProps) {
  return (
    <IconButton aria-label="delete" color="secondary" {...props}>
      <DeleteIcon />
    </IconButton>
  );
}
