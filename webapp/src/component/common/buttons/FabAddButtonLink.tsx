import { Box } from '@mui/material';
import Fab from '@mui/material/Fab';
import AddIcon from '@mui/icons-material/Add';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

export function FabAddButtonLink(props: { to: string }) {
  const { t } = useTranslate();

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
