import IconButton from '@material-ui/core/IconButton';
import EditIcon from '@material-ui/icons/Edit';

export function EditIconButton(props) {
  return (
    <IconButton aria-label="edit" color="primary" {...props}>
      <EditIcon />
    </IconButton>
  );
}
