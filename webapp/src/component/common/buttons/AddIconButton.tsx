import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import * as React from 'react';

export function AddIconButton(props: any) {
  return (
    <IconButton aria-label="edit" color="primary" {...props}>
      <AddIcon />
    </IconButton>
  );
}
