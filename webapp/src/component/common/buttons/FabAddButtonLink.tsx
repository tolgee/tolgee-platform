import { Box } from '@material-ui/core';
import Fab from '@material-ui/core/Fab';
import AddIcon from '@material-ui/icons/Add';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

export function FabAddButtonLink(props: { to: string }) {
  const t = useTranslate();

  return (
    <Box mt={2}>
      <Fab
        color="primary"
        data-cy="global-plus-button"
        aria-label={t('button_add_aria_label')}
        component={Link}
        {...props}
      >
        <AddIcon />
      </Fab>
    </Box>
  );
}
