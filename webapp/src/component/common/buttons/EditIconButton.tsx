import IconButton from '@mui/material/IconButton';
import EditIcon from '@mui/icons-material/Edit';

export function EditIconButton(props) {
  return (
    <IconButton aria-label="edit" color="primary" size="large" {...props}>
      <EditIcon />
    </IconButton>
  );
}
