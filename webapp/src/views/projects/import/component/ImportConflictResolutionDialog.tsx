import React, { FunctionComponent } from 'react';
import AppBar from '@mui/material/AppBar';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import Slide from '@mui/material/Slide';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { Theme } from '@mui/material/styles';
import createStyles from '@mui/styles/createStyles';
import makeStyles from '@mui/styles/makeStyles';
import { TransitionProps } from '@mui/material/transitions';
import CloseIcon from '@mui/icons-material/Close';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

import { ImportConflictsData } from './ImportConflictsData';

container.resolve(ImportActions);
const useStyles = makeStyles<Theme>((theme) =>
  createStyles({
    appBar: {
      position: 'relative',
    },
    title: {
      marginLeft: theme.spacing(2),
      flex: 1,
    },
  })
);

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & { children: React.ReactElement },
  ref: React.Ref<unknown>
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export const ImportConflictResolutionDialog: FunctionComponent<{
  row?: components['schemas']['ImportLanguageModel'];
  onClose: () => void;
}> = (props) => {
  const classes = useStyles();

  return (
    <div>
      <Dialog
        fullScreen
        open={!!props.row}
        onClose={props.onClose}
        TransitionComponent={Transition}
        data-cy="import-conflict-resolution-dialog"
      >
        <AppBar className={classes.appBar}>
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={props.onClose}
              aria-label="close"
              data-cy="import-resolution-dialog-close-button"
              size="large"
            >
              <CloseIcon />
            </IconButton>
            <Typography variant="h6" className={classes.title}>
              <T>import_resolve_conflicts_title</T>
            </Typography>
          </Toolbar>
        </AppBar>
        {!!props.row && <ImportConflictsData row={props.row} />}
      </Dialog>
    </div>
  );
};
