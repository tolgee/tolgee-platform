import React, { FunctionComponent } from 'react';
import {
  FormControlLabel,
  Grid,
  styled,
  Switch,
  Typography,
} from '@mui/material';
import { CheckCircle, AlertTriangle } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import clsx from 'clsx';

import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { components } from 'tg.service/apiSchema.generated';

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

export const ImportConflictsSecondaryBar: FunctionComponent<{
  onShowResolvedToggle: () => void;
  showResolved: boolean;
  languageData: components['schemas']['ImportLanguageModel'];
}> = (props) => {
  const resolvedCount = props.languageData.resolvedCount;

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

            <AlertTriangle className={clsx('validIcon', 'conflictsIcon')} />

            <Typography
              variant="body1"
              data-cy="import-resolution-dialog-conflict-count"
            >
              {props.languageData.conflictCount}
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
            label={<T keyName="import_conflicts_filter_show_resolved_label" />}
          />
        </Grid>
      </Grid>
    </SecondaryBar>
  );
};
