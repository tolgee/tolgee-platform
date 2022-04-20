import IconButton from '@mui/material/IconButton';
import AddIcon from '@mui/icons-material/Add';

export function AddIconButton(props: any) {
  return (
    <IconButton aria-label="edit" color="primary" size="large" {...props}>
      <AddIcon />
    </IconButton>
  );
}
