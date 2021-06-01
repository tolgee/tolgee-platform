import IconButton from '@material-ui/core/IconButton';
import * as React from 'react';
import AddIcon from '@material-ui/icons/Add';

export function AddIconButton(props: any) {
  return (
    <IconButton aria-label="edit" color="primary" {...props}>
      <AddIcon />
    </IconButton>
  );
}
