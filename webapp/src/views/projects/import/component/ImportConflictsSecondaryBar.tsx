import React, { FunctionComponent } from 'react';
import {
  FormControlLabel,
  Grid,
  styled,
  Switch,
  Typography,
} from '@mui/material';
import { CheckCircle, Warning } from '@mui/icons-material';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { container } from 'tsyringe';

import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { ImportActions } from 'tg.store/project/ImportActions';

const StyledCounter = styled('div')`
  display: flex;
  align-items: center;
  border-radius: 20px;

  & .resolvedIcon {
    color: ${({ theme }) => theme.palette.success.main};
  }

  & .validIcon {
    font-size: 20px;
    margin-right: 4px;
  }

  & .conflictsIcon {
    margin-left: ${({ theme }) => theme.spacing(2)};
    color: ${({ theme }) => theme.palette.warning.main};
  }
`;

const actions = container.resolve(ImportActions);
export const ImportConflictsSecondaryBar: FunctionComponent<{
  onShowResolvedToggle: () => void;
  showResolved: boolean;
}> = (props) => {
  const languageDataLoadable = actions.useSelector(
    (s) => s.loadables.resolveConflictsLanguage
  );
  const resolvedCount = languageDataLoadable.data?.resolvedCount;

  return (
    <SecondaryBar>
      <Grid container spacing={4} alignItems="center">
        <Grid item>
          <StyledCounter>
            <CheckCircle className={clsx('validIcon', 'resolvedIcon')} />
            <Typography
              variant="body1"
              data-cy="import-resolution-dialog-resolved-count"
            >
              {resolvedCount !== undefined ? resolvedCount : '??'}
            </Typography>

            <Warning className={clsx('validIcon', 'conflictsIcon')} />

            <Typography
              variant="body1"
              data-cy="import-resolution-dialog-conflict-count"
            >
              {languageDataLoadable.data?.conflictCount}
            </Typography>
          </StyledCounter>
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
