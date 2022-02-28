import React, { FunctionComponent } from 'react';
import AppBar from '@material-ui/core/AppBar';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import Slide from '@material-ui/core/Slide';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import { createStyles, makeStyles, Theme } from '@material-ui/core/styles';
import { TransitionProps } from '@material-ui/core/transitions';
import CloseIcon from '@material-ui/icons/Close';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

import { ImportConflictsData } from './ImportConflictsData';

container.resolve(ImportActions);
const useStyles = makeStyles((theme: Theme) =>
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
  props: TransitionProps & { children?: React.ReactElement },
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
