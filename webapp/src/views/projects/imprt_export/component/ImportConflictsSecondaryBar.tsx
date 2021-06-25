import React, { FunctionComponent } from 'react';
import {
  Box,
  FormControlLabel,
  Grid,
  makeStyles,
  Switch,
  Typography,
} from '@material-ui/core';
import { CheckCircle, Warning } from '@material-ui/icons';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { container } from 'tsyringe';

import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { ImportActions } from 'tg.store/project/ImportActions';

const useStyles = makeStyles((theme) => ({
  counter: {
    display: 'flex',
    alignItems: 'center',
    borderRadius: 20,
  },
  validIcon: {
    fontSize: 20,
    marginRight: 4,
  },
  resolvedIcon: {
    color: theme.palette.success.main,
  },
  conflictsIcon: {
    marginLeft: theme.spacing(2),
    color: theme.palette.warning.main,
  },
}));

const actions = container.resolve(ImportActions);
export const ImportConflictsSecondaryBar: FunctionComponent<{
  onShowResolvedToggle: () => void;
  showResolved: boolean;
}> = (props) => {
  const languageDataLoadable = actions.useSelector(
    (s) => s.loadables.resolveConflictsLanguage
  );

  const classes = useStyles();
  const resolvedCount = languageDataLoadable.data?.resolvedCount;

  return (
    <SecondaryBar>
      <Grid container spacing={4} alignItems="center">
        <Grid item>
          <Box className={classes.counter}>
            <CheckCircle
              className={clsx(classes.validIcon, classes.resolvedIcon)}
            />

            <Typography
              variant="body1"
              data-cy="import-resolution-dialog-resolved-count"
            >
              {resolvedCount !== undefined ? resolvedCount : '??'}
            </Typography>

            <Warning
              className={clsx(classes.validIcon, classes.conflictsIcon)}
            />

            <Typography
              variant="body1"
              data-cy="import-resolution-dialog-conflict-count"
            >
              {languageDataLoadable.data?.conflictCount}
            </Typography>
          </Box>
        </Grid>
        <Grid item>
          <FormControlLabel
            control={
              <Switch
                data-cy="import-resolution-dialog-show-resolved-switch"
                checked={props.showResolved}
                onChange={props.onShowResolvedToggle}
                name="filter_unresolved"
                color="primary"
              />
            }
            label={<T>import_conflicts_filter_show_resolved_label</T>}
          />
        </Grid>
      </Grid>
    </SecondaryBar>
  );
};
